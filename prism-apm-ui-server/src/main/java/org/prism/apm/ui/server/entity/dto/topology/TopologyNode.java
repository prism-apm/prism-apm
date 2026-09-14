package org.prism.apm.ui.server.entity.dto.topology;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class TopologyNode {

    private String id;
    private String label;
    private List<Integer> size;
    private String type;
    private Integer layer;
    private String img;
    private NodeLabelCfg labelCfg;

    //==Properties below are not for G6==
    private String nodeComponent;

    @Data
    @Builder
    public static class NodeLabelCfg {
        private LabelStyle style;
    }

}
