package org.prism.apm.ui.server.entity.response.topology;

import org.prism.apm.ui.server.entity.dto.topology.TopologyEdge;
import org.prism.apm.ui.server.entity.dto.topology.TopologyNode;
import org.prism.apm.ui.server.entity.dto.topology.TopologyWarningResult;
import lombok.Data;

import java.util.List;

@Data
public class TopologyGraphDataResponse {

    private List<TopologyNode> nodes;
    private List<TopologyEdge> edges;
    private List<TopologyWarningResult> highlights;

}
