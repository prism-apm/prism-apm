package org.prism.apm.ui.server.service.topology;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.prism.apm.ui.server.constant.CommonConstants;
import org.prism.apm.ui.server.constant.SpanConstants;
import org.prism.apm.ui.server.constant.route.DynamicTablePlaceholder;
import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.entity.dto.topology.TopologyEdge;
import org.prism.apm.ui.server.entity.dto.topology.LabelStyle;
import org.prism.apm.ui.server.entity.dto.topology.TopologyNode;
import org.prism.apm.ui.server.entity.dto.topology.TopologyWarningResult;
import org.prism.apm.ui.server.entity.model.topology.ServiceTopologyRelationView;
import org.prism.apm.ui.server.entity.model.topology.TopologyWarningRule;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.enums.SystemTypeEnum;
import org.prism.apm.ui.server.enums.topology.NodeComponent;
import org.prism.apm.ui.server.mapper.topology.TopologyDataQueryMapper;
import org.prism.apm.ui.server.route.TableRoutingService;
import org.prism.apm.ui.server.service.service.CommonService;
import org.prism.apm.ui.server.service.topology.strategy.SimpleAlertInfluenceCalculator;
import org.prism.apm.ui.server.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class TopologyDataQueryService {

    private static final String MARKER_NODE_TYPE_JDBC = "JDBC";
    private static final String MARKER_NODE_TYPE_REDIS = "Redis";
    private static final String MARKER_NODE_TYPE_MQ_ROCKET = "ROCKET";
    private static final String MARKER_NODE_TYPE_MQ_KAFKA = "KAFKA";

    private final TopologyWarningRuleService topologyWarningRuleService;
    private final TopologyDataQueryMapper topologyDataQueryMapper;
    private final CommonService commonService;
    private final TableRoutingService tableRoutingService;

    private final Predicate<ServiceTopologyRelationView> non_empty_value_caller = s -> !SpanConstants.DEFAULT_STRING_VALUE_WHEN_NO_DATA.equals(s.getCallerServiceName());

    public TopologyDataQueryService(TopologyWarningRuleService topologyWarningRuleService, TopologyDataQueryMapper topologyDataQueryMapper, CommonService commonService, TableRoutingService tableRoutingService) {
        this.topologyWarningRuleService = topologyWarningRuleService;
        this.topologyDataQueryMapper = topologyDataQueryMapper;
        this.commonService = commonService;
        this.tableRoutingService = tableRoutingService;
    }

    public List<TopologyEdge> getEdgesData(DateParam dateParam) {
        List<TopologyEdge> edges = new ArrayList<>();
        List<String> current_user_service_list = commonService.getCurrentUserServiceNameList();
        if (current_user_service_list.isEmpty()) {
            return edges;
        }
        RouteInfo routeInfo = tableRoutingService.getTableName(dateParam, DynamicTablePlaceholder.TAG_TOPOLOGY);
        log.info("topology table route => {}", routeInfo.getDynamicTableName());

        List<TopologyEdge> rpc_edges = topologyDataQueryMapper.selectRpc(dateParam, current_user_service_list, routeInfo).stream()
                .filter(non_empty_value_caller)
                .map(
                        s -> {
                            TopologyEdge edge = new TopologyEdge();
                            edge.setSource(s.getCallerServiceName());
                            edge.setTarget(s.getServiceName());
                            return edge;
                        }
                ).toList();


        List<TopologyEdge> jdbc_edges = topologyDataQueryMapper.selectJdbc(dateParam, current_user_service_list, routeInfo).stream()
                .filter(non_empty_value_caller)
                .map(
                        s -> {
                            TopologyEdge edge = new TopologyEdge();
                            edge.setSource(s.getCallerServiceName());
                            edge.setTarget(String.format("[%s]%s", MARKER_NODE_TYPE_JDBC, s.getServiceName()));
                            return edge;
                        }
                )
                .toList();


        List<TopologyEdge> redis_edges = topologyDataQueryMapper.selectRedis(dateParam, current_user_service_list, routeInfo).stream()
                .filter(non_empty_value_caller)
                .map(s -> {
                    TopologyEdge edge = new TopologyEdge();
                    edge.setSource(s.getCallerServiceName());
                    edge.setTarget(String.format("[%s]%s", MARKER_NODE_TYPE_REDIS, s.getServiceName()));
                    return edge;
                })
                .toList();


        List<ServiceTopologyRelationView> producer_data = topologyDataQueryMapper.selectMqProducer(dateParam, current_user_service_list, routeInfo);
        List<TopologyEdge> producer_edges_rocket = producer_data.stream()
                .filter(non_empty_value_caller)
                .filter(s -> s.getSystemType().contains(SystemTypeEnum.ROCKETMQ.getName()))
                .map(s -> {
                    TopologyEdge edge = new TopologyEdge();
                    edge.setSource(s.getCallerServiceName());
                    edge.setTarget(String.format("[%s]%s", MARKER_NODE_TYPE_MQ_ROCKET, s.getServiceName()));
                    return edge;
                })
                .toList();
        List<TopologyEdge> producer_edges_kafka = producer_data.stream()
                .filter(non_empty_value_caller)
                .filter(s -> s.getSystemType().contains(SystemTypeEnum.KAFKA.getName()))
                .map(s -> {
                    TopologyEdge edge = new TopologyEdge();
                    edge.setSource(s.getCallerServiceName());
                    edge.setTarget(String.format("[%s]%s", MARKER_NODE_TYPE_MQ_KAFKA, s.getServiceName()));
                    return edge;
                })
                .toList();

        List<ServiceTopologyRelationView> consumer_data = topologyDataQueryMapper.selectMqConsumer(dateParam, current_user_service_list, routeInfo);
        //Consumer data is in A->B form
        //This means it can provide both the MQ->B consume relationship and the A->MQ produce relationship
        List<TopologyEdge> producer_edges_from_consumer_relation_rocket = consumer_data.stream()
                .filter(non_empty_value_caller)
                .filter(s -> s.getSystemType().contains(SystemTypeEnum.ROCKETMQ.getName()))
                .map(s -> {
                    TopologyEdge edge = new TopologyEdge();
                    edge.setSource(s.getCallerServiceName());
                    edge.setTarget(String.format("[%s]%s", MARKER_NODE_TYPE_MQ_ROCKET, s.getServiceName()));
                    return edge;
                })
                .toList();
        List<TopologyEdge> consumer_edges_rocket = consumer_data.stream()
                .filter(non_empty_value_caller)
                .filter(s -> s.getSystemType().contains(SystemTypeEnum.ROCKETMQ.getName()))
                .map(s -> {
                    TopologyEdge edge = new TopologyEdge();
                    edge.setSource(String.format("[%s]%s", MARKER_NODE_TYPE_MQ_ROCKET, s.getCallerServiceName()));
                    edge.setTarget(s.getServiceName());
                    return edge;
                })
                .toList();
        List<TopologyEdge> producer_edges_from_consumer_relation_kafka = consumer_data.stream()
                .filter(non_empty_value_caller)
                .filter(s -> s.getSystemType().contains(SystemTypeEnum.KAFKA.getName()))
                .map(s -> {
                    TopologyEdge edge = new TopologyEdge();
                    edge.setSource(s.getCallerServiceName());
                    edge.setTarget(String.format("[%s]%s", MARKER_NODE_TYPE_MQ_KAFKA, s.getServiceName()));
                    return edge;
                })
                .toList();
        List<TopologyEdge> consumer_edges_kafka = consumer_data.stream()
                .filter(non_empty_value_caller)
                .filter(s -> s.getSystemType().contains(SystemTypeEnum.KAFKA.getName()))
                .map(s -> {
                    TopologyEdge edge = new TopologyEdge();
                    edge.setSource(String.format("[%s]%s", MARKER_NODE_TYPE_MQ_KAFKA, s.getCallerServiceName()));
                    edge.setTarget(s.getServiceName());
                    return edge;
                })
                .toList();

        //Summarize edges
        edges.addAll(rpc_edges);
        edges.addAll(jdbc_edges);
        edges.addAll(redis_edges);
        edges.addAll(producer_edges_rocket);
        edges.addAll(producer_edges_kafka);
        edges.addAll(producer_edges_from_consumer_relation_rocket);
        edges.addAll(consumer_edges_rocket);
        edges.addAll(producer_edges_from_consumer_relation_kafka);
        edges.addAll(consumer_edges_kafka);

        edges.forEach(e -> e.setId(String.format("%s::%s", e.getSource(), e.getTarget())));
        edges = edges.stream().distinct().collect(Collectors.toList());

        return edges;
    }

    /**
     * Extract and build node data from the edge list
     *
     * @param edges Edge list used to extract node information
     * @return List containing all unique nodes
     */
    public List<TopologyNode> getNodesData(List<TopologyEdge> edges) {
        return edges.stream()
                .flatMap(e -> Stream.of(e.getSource(), e.getTarget()))
                .distinct()
                .map(id -> {
                    TopologyNode node = new TopologyNode();
                    node.setId(id);
                    node.setLabel(insertLineBreaks(id, 18));
                    return node;
                })
                .collect(Collectors.toList());
    }

    /**
     * Insert newline characters after the string exceeds the specified length.
     *
     * @param str     Original string
     * @param maxLength Maximum characters per line
     * @return String after newline insertion
     */
    private static String insertLineBreaks(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        // Split the string by fixed length
        Iterable<String> parts = Splitter.fixedLength(maxLength).split(str);
        // Join split parts with newline characters
        return String.join("\n", parts);
    }


    public void handleNodesComponent(List<TopologyNode> nodes) {
        for (TopologyNode node : nodes) {
            String node_id = node.getId();
            if (isJDBCNode(node_id)) {
                node.setNodeComponent(NodeComponent.JDBC.name());
            } else if (isRedisNode(node_id)) {
                node.setNodeComponent(NodeComponent.Redis.name());
            } else if (isMqNode(node_id)) {
                node.setNodeComponent(NodeComponent.MQ.name());
            } else {
                node.setNodeComponent(NodeComponent.Service.name());
            }
        }
    }

    public void handleNodesType(List<TopologyNode> nodes) {
        for (TopologyNode node : nodes) {
            node.setType("image");
            node.setSize(Lists.newArrayList(70, 70));
        }
    }

    public void handleNodesLabelCfg(List<TopologyNode> nodes) {
        nodes.forEach(node -> node.setLabelCfg(TopologyNode.NodeLabelCfg.builder()
                .style(LabelStyle.builder()
                        .fill("black")
                        .fontSize(18)
                        .lineWidth(3)
                        .stroke("")
                        .build())
                .build()));
    }

    private boolean isJDBCNode(String nodeName) {
        return nodeName.contains(MARKER_NODE_TYPE_JDBC);
    }

    private boolean isRedisNode(String nodeName) {
        return nodeName.contains(MARKER_NODE_TYPE_REDIS);
    }

    private boolean isMqNode(String nodeName) {
        return nodeName.contains(MARKER_NODE_TYPE_MQ_ROCKET) || nodeName.contains(MARKER_NODE_TYPE_MQ_KAFKA);
    }


    /**
     * Get topology highlight data for the time range.
     *
     * @param dateParam Time range; the format must meet database query requirements
     * @return List of topology warning results containing highlighted nodes and edges; returns an empty list when there are no rules or no rule is triggered
     */
    public List<TopologyWarningResult> getHighlightData(DateParam dateParam) {

        List<TopologyWarningResult> highlight_results = new ArrayList<>();

        TopologyWarningRule topologyWarningRule = new TopologyWarningRule();
        topologyWarningRule.setEnabled(CommonConstants.TRUE);
        List<TopologyWarningRule> rule_list = topologyWarningRuleService.getRuleList(topologyWarningRule);
        if (rule_list.isEmpty()) {
            return null;
        }

        for (TopologyWarningRule rule : rule_list) {

            String threshold = convertJsonToSqlWhereClause(rule.getRuleDetails());
            if (!StringUtils.hasText(threshold)) {
                continue;
            }

            // Determine whether the service triggered the rule
            List<String> services_above_threshold = topologyDataQueryMapper.selectServicesAboveThreshold(dateParam, rule.getServiceName(), threshold);
            if (CollectionUtils.isEmpty(services_above_threshold)) {
                //next rule ^^
                continue;
            }

            // Calculate each metric influence percentage for highlight intensity
            List<TopologyWarningResult.TopologyWarningEdge> topology_warning_edges = this.computeEdgeHighlightForUpstreamDownstream(dateParam, convertListToSqlInClause(services_above_threshold), threshold, rule);

            //prepare result
            TopologyWarningResult one_rule_highlight_result = new TopologyWarningResult();
            one_rule_highlight_result.setRule(rule);
            one_rule_highlight_result.setHighlightNodes(services_above_threshold.stream().map(
                    name -> {
                        TopologyNode node = new TopologyNode();
                        node.setId(name);
                        node.setLabel(name);
                        return node;
                    }
            ).collect(Collectors.toList()));
            one_rule_highlight_result.setHighlightEdges(topology_warning_edges);

            highlight_results.add(one_rule_highlight_result);
        }


        return highlight_results;
    }

    /**
     * Calculate alert-highlighted edges for upstream and downstream service relationships
     * @param dateParam          Time range
     * @param serviceName    Service name
     * @param threshold      Threshold condition used to filter call relationships that exceed the threshold
     * @param rule           Alert rule object used to calculate metric influence percentage
     * @return List containing upstream and downstream highlighted edges, combined with metric influence calculation results
     */
    private List<TopologyWarningResult.TopologyWarningEdge> computeEdgeHighlightForUpstreamDownstream(DateParam dateParam, String serviceName, String threshold, TopologyWarningRule rule){
        // Query downstream and upstream call relationships that exceed the threshold
        List<ServiceTopologyRelationView> down_relations = topologyDataQueryMapper.selectOverThresholdDownstream(dateParam, serviceName, threshold);
        List<ServiceTopologyRelationView> up_relations = topologyDataQueryMapper.selectOverThresholdUpstream(dateParam, serviceName, threshold);

        // Convert raw call relationships to alert edge objects
        List<TopologyWarningResult.TopologyWarningEdge> down_edges = convertRelation2WarningEdges(down_relations);
        List<TopologyWarningResult.TopologyWarningEdge> up_edges = convertRelation2WarningEdges(up_relations);

        // Calculate metric influence percentage for highlight color and numeric display
        SimpleAlertInfluenceCalculator.calculateSingleInfluence(rule, down_edges, Sets.newHashSet(SpanConstants.CNT));
        // Exclude error-count and time-based metrics from influence calculation for upstream relationships
        Set<String> exclude_when_up = new HashSet<>();
        exclude_when_up.add(SpanConstants.ERROR_NUM);
        exclude_when_up.addAll(SpanConstants.DURATION_INDICATORS);
        SimpleAlertInfluenceCalculator.calculateSingleInfluence(rule, up_edges, exclude_when_up);

        // Merge upstream and downstream edge data and return
        down_edges.addAll(up_edges);
        return down_edges;
    }


    private static List<TopologyWarningResult.TopologyWarningEdge> convertRelation2WarningEdges(List<ServiceTopologyRelationView> relations) {
        if (CollectionUtils.isEmpty(relations)) {
            return new ArrayList<>();
        }
        return relations.stream()
                .map(
                        relation -> {
                            TopologyWarningResult.TopologyWarningEdge edge = new TopologyWarningResult.TopologyWarningEdge();
                            edge.setSource(relation.getCallerServiceName());
                            String target = processServiceToTargetFormat(relation);
                            edge.setTarget(target);
                            edge.setAvgDuration(relation.getAvgDuration());
                            edge.setMaxDuration(relation.getMaxDuration());
                            edge.setMinDuration(relation.getMinDuration());
                            edge.setCnt(relation.getCnt());
                            edge.setErrorNum(relation.getErrorNum());
                            edge.setP90Duration(relation.getP90Duration());
                            edge.setP95Duration(relation.getP95Duration());
                            edge.setP99Duration(relation.getP99Duration());
                            return edge;
                        }
                )
                .filter(edge -> !SpanConstants.DEFAULT_STRING_VALUE_WHEN_NO_DATA.equals(edge.getSource()) && !SpanConstants.DEFAULT_STRING_VALUE_WHEN_NO_DATA.equals(edge.getTarget()))
                .filter(edge -> !edge.getSource().equals(edge.getTarget()))
                .collect(Collectors.toList());
    }


    private static String processServiceToTargetFormat(ServiceTopologyRelationView relation) {
        String service_name = relation.getServiceName();
        if (SystemTypeEnum.MYSQL.getName().equals(relation.getSystemType())) {
            service_name = String.format("[%s]%s", MARKER_NODE_TYPE_JDBC, service_name);
        } else if (SystemTypeEnum.REDIS.getName().equals(relation.getSystemType())) {
            service_name = String.format("[%s]%s", MARKER_NODE_TYPE_REDIS, service_name);
        } else if (SystemTypeEnum.KAFKA.getName().equals(relation.getSystemType())) {
            service_name = String.format("[%s]%s", MARKER_NODE_TYPE_MQ_KAFKA, service_name);
        } else if (SystemTypeEnum.ROCKETMQ.getName().equals(relation.getSystemType())) {
            service_name = String.format("[%s]%s", MARKER_NODE_TYPE_MQ_ROCKET, service_name);
        }
        return service_name;
    }

    /**
     * Convert string list to a format suitable for SQL IN clauses
     *
     * @param values String list
     * @return Formatted string suitable for SQL IN clauses
     */
    public static String convertListToSqlInClause(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        StringBuilder inClause = new StringBuilder();
        for (String value : values) {
            if (!inClause.isEmpty()) {
                inClause.append(", ");
            }
            inClause.append("'").append(value).append("'");
        }

        return inClause.toString();
    }

    /**
     * Convert a JSON-format condition list to an SQL WHERE clause (fixed version: logical operators are added only before non-final conditions)
     *
     * @param ruleDetails JSON-format condition list string
     * @return SQL WHERE clause string
     */
    private static String convertJsonToSqlWhereClause(String ruleDetails) {
        if (ruleDetails == null || ruleDetails.trim().isEmpty()) {
            return "";
        }

        List<TopologyWarningRule.RuleDetails> conditions;

        try {
            conditions = JsonUtils.parseArray(ruleDetails, TopologyWarningRule.RuleDetails.class);
        } catch (Exception e) {
            log.error("JSON parsing failed: ", e);
            return "";
        }

        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        // Define metric set requiring unit conversion

        StringBuilder whereClause = new StringBuilder();

        for (int i = 0; i < conditions.size(); i++) {
            TopologyWarningRule.RuleDetails condition = conditions.get(i);
            String indicatorName = condition.getIndicatorName();
            String operator = condition.getOperator();
            String value = condition.getValue();
            String logicalOperator = condition.getLogicalOperator();

            if (indicatorName == null || operator == null || value == null) {
                log.error("Incomplete condition configuration: indicatorName, operator, and value cannot be empty. Current condition: {}", condition);
                continue;
            }

            // Handle value conversion
            String processedValue = value;
            if (SpanConstants.DURATION_INDICATORS.contains(indicatorName)) {
                try {
                    // Convert milliseconds to microseconds (1 ms = 1000 us)
                    long msValue = Long.parseLong(value);
                    long usValue = msValue * 1000;
                    processedValue = String.valueOf(usValue);
                } catch (NumberFormatException e) {
                    log.error("Value conversion failed; value must be numeric: {}", value);
                    continue;
                }
            }

            // Build condition expression
            whereClause.append(indicatorName)
                    .append(" ")
                    .append(operator)
                    .append(" ")
                    .append(processedValue);

            // Add logical operator
            if (i < conditions.size() - 1 && logicalOperator != null && !logicalOperator.trim().isEmpty()) {
                whereClause.append(" ").append(logicalOperator.trim().toUpperCase()).append(" ");
            }
        }

        return whereClause.toString();
    }

}
