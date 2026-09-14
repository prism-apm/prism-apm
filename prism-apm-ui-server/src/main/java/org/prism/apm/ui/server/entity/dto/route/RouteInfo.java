package org.prism.apm.ui.server.entity.dto.route;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteInfo {

    private String dynamicTableName;
    private int dataStep;

}
