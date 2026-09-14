package org.prism.apm.ui.server.service.topology.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.prism.apm.ui.server.entity.model.topology.TopologyWarningRule;
import org.prism.apm.ui.server.entity.dto.topology.TopologyWarningResult;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleAlertInfluenceCalculator {

    public static void calculateSingleInfluence(TopologyWarningRule rule,
                                                List<TopologyWarningResult.TopologyWarningEdge> highlightEdges,
                                                Set<String> excludedIndicators) {
        if (rule == null || CollectionUtils.isEmpty(highlightEdges)) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();

        // Parse rules
        List<TopologyWarningRule.RuleDetails> ruleDetails;
        try {
            ruleDetails = Arrays.asList(
                    mapper.readValue(rule.getRuleDetails(), TopologyWarningRule.RuleDetails[].class)
            );
        } catch (JsonProcessingException e) {
            return;
        }

        // Filter out excluded metrics
        List<TopologyWarningRule.RuleDetails> filteredRuleDetails = new ArrayList<>();
        for (TopologyWarningRule.RuleDetails rd : ruleDetails) {
            if (!excludedIndicators.contains(rd.getIndicatorName())) {
                filteredRuleDetails.add(rd);
            }
        }

        // 1. Calculate totals for all monitored metrics, considering only non-excluded metrics
        Map<String, Double> indicatorTotals = new HashMap<>();
        for (TopologyWarningRule.RuleDetails rd : filteredRuleDetails) {
            double total = highlightEdges.stream()
                    .mapToDouble(e -> getIndicatorValue(e, rd).doubleValue())
                    .sum();
            indicatorTotals.put(rd.getIndicatorName(), total);
        }

        // 2. Calculate each edge metric influence and total influence, considering only non-excluded metrics
        double totalInfluenceSum = 0.0;
        List<Double> edgeInfluences = new ArrayList<>();

        for (TopologyWarningResult.TopologyWarningEdge edge : highlightEdges) {
            double edgeInfluence = 0.0;

            for (TopologyWarningRule.RuleDetails rd : filteredRuleDetails) {
                String indicatorName = rd.getIndicatorName();
                double value = getIndicatorValue(edge, rd).doubleValue();
                double total = indicatorTotals.getOrDefault(indicatorName, 1.0); // Avoid division by zero
                double indicatorInfluence = total > 0 ? value / total : 0.0;

                setIndicatorInfluence(edge, indicatorName, BigDecimal.valueOf(indicatorInfluence));
                edgeInfluence += indicatorInfluence;
            }

            edgeInfluences.add(edgeInfluence);
            totalInfluenceSum += edgeInfluence;
        }

        // 3. Set total influence
        if (totalInfluenceSum > 0) {
            for (int i = 0; i < highlightEdges.size(); i++) {
                TopologyWarningResult.TopologyWarningEdge edge = highlightEdges.get(i);
                double influencePercentage = edgeInfluences.get(i) / totalInfluenceSum;
                edge.setInfluencePercentage(BigDecimal.valueOf(influencePercentage));
            }
        }
    }

    private static BigDecimal getIndicatorValue(TopologyWarningResult.TopologyWarningEdge edge, TopologyWarningRule.RuleDetails rule) {
        String indicator = rule.getIndicatorName();
        String operator = rule.getOperator();

        switch (indicator) {
            case "cnt":
                Long cntValue = edge.getCnt();
                // When the operator contains <, smaller values have greater influence, so use the reciprocal form
                if (operator.contains("<")) {
                    return cntValue > 0 ? BigDecimal.ONE.divide(BigDecimal.valueOf(cntValue), RoundingMode.HALF_UP) : BigDecimal.valueOf(Double.MAX_VALUE); // Avoid division by zero
                }
                return BigDecimal.valueOf(cntValue);
            case "avg_duration":
                return edge.getAvgDuration();
            case "max_duration":
                return edge.getMaxDuration();
            case "min_duration":
                return edge.getMinDuration();
            case "error_num":
                return BigDecimal.valueOf(edge.getErrorNum());
            case "p90_duration":
                return edge.getP90Duration();
            case "p95_duration":
                return edge.getP95Duration();
            case "p99_duration":
                return edge.getP99Duration();
            default:
                return BigDecimal.ZERO;
        }
    }

    private static void setIndicatorInfluence(TopologyWarningResult.TopologyWarningEdge edge,
                                              String indicator,
                                              BigDecimal influence) {
        switch (indicator) {
            case "cnt":
                edge.setInfluencePercentage_cnt(influence);
                break;
            case "avg_duration":
                edge.setInfluencePercentage_avgDuration(influence);
                break;
            case "max_duration":
                edge.setInfluencePercentage_maxDuration(influence);
                break;
            case "min_duration":
                edge.setInfluencePercentage_minDuration(influence);
                break;
            case "error_num":
                edge.setInfluencePercentage_errorNum(influence);
                break;
            case "p90_duration":
                edge.setInfluencePercentage_p90Duration(influence);
                break;
            case "p95_duration":
                edge.setInfluencePercentage_p95Duration(influence);
                break;
            case "p99_duration":
                edge.setInfluencePercentage_p99Duration(influence);
                break;
        }
    }
}