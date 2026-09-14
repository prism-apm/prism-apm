package org.prism.apm.ui.server.entity.dto.topology;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabelStyle {

    private String fill;//Text color
    private int fontSize;
    private int lineWidth;
    private String stroke;

}
