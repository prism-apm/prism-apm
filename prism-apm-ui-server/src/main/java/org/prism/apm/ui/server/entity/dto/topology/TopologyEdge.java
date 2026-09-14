package org.prism.apm.ui.server.entity.dto.topology;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TopologyEdge {

    @EqualsAndHashCode.Include
    private String id;
    private String source;
    private String target;
    private String type;
    private String label;

}
