package org.prism.apm.ui.server.entity.dto.topology;

import org.prism.apm.ui.server.entity.model.topology.TopologyWarningRule;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TopologyWarningResult {

    private TopologyWarningRule rule;
    private List<TopologyNode> highlightNodes;
    private List<TopologyWarningEdge> highlightEdges;

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class TopologyWarningEdge extends TopologyEdge {
        private BigDecimal avgDuration;
        private BigDecimal maxDuration;
        private BigDecimal minDuration;
        private Long cnt;
        private Long errorNum;
        private BigDecimal p90Duration;
        private BigDecimal p95Duration;
        private BigDecimal p99Duration;

        // Total alert influence
        private BigDecimal influencePercentage;

        // Alert influence
        private BigDecimal influencePercentage_avgDuration;
        private BigDecimal influencePercentage_maxDuration;
        private BigDecimal influencePercentage_minDuration;
        private BigDecimal influencePercentage_cnt;
        private BigDecimal influencePercentage_errorNum;
        private BigDecimal influencePercentage_p90Duration;
        private BigDecimal influencePercentage_p95Duration;
        private BigDecimal influencePercentage_p99Duration;
    }

}
