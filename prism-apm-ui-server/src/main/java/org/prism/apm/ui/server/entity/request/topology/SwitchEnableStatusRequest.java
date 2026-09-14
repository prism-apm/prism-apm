package org.prism.apm.ui.server.entity.request.topology;

import lombok.Data;

@Data
public class SwitchEnableStatusRequest {
    // Getters and Setters
    private Integer id;
    private Boolean isEnable;
}
