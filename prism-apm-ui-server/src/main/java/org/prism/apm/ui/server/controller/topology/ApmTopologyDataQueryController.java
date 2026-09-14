package org.prism.apm.ui.server.controller.topology;

import org.prism.apm.ui.server.entity.dto.topology.TopologyEdge;
import org.prism.apm.ui.server.entity.dto.topology.TopologyNode;
import org.prism.apm.ui.server.entity.dto.topology.TopologyWarningResult;
import org.prism.apm.ui.server.entity.param.common.DateParam;
import org.prism.apm.ui.server.entity.response.ViewResponse;
import org.prism.apm.ui.server.entity.response.topology.TopologyGraphDataResponse;
import org.prism.apm.ui.server.service.topology.TopologyDataQueryService;
import org.prism.apm.ui.server.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apm/topology/topology-data")
@Slf4j
public class ApmTopologyDataQueryController {

    private final TopologyDataQueryService topologyDataQueryService;

    public ApmTopologyDataQueryController(TopologyDataQueryService topologyDataQueryService) {
        this.topologyDataQueryService = topologyDataQueryService;
    }

    @GetMapping("/getData")
    public ViewResponse.Result<TopologyGraphDataResponse> getData(DateParam dateParam) {
        log.info("get APM Topology start Range: ===============> {}", JsonUtils.toJson(dateParam));
        dateParam.convertToUTC();
        List<TopologyEdge> edges = topologyDataQueryService.getEdgesData(dateParam);
        List<TopologyNode> nodes = topologyDataQueryService.getNodesData(edges);
        //Handle node component type
        topologyDataQueryService.handleNodesComponent(nodes);
        //Handle node shape
        topologyDataQueryService.handleNodesType(nodes);
        //Handle node label style
        topologyDataQueryService.handleNodesLabelCfg(nodes);

        TopologyGraphDataResponse result = new TopologyGraphDataResponse();
        result.setEdges(edges);
        result.setNodes(nodes);

        //get highlight data
        List<TopologyWarningResult> highlight_data = topologyDataQueryService.getHighlightData(dateParam);
        result.setHighlights(highlight_data);

        return ViewResponse.Result.ok(result);
    }








}
