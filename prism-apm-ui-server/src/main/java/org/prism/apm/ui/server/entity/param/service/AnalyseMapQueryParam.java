package org.prism.apm.ui.server.entity.param.service;

import lombok.Data;

@Data
public class AnalyseMapQueryParam {

    private String serviceName;
    private String ifaceName;
    private String ifaceNameHashKey;
    private String machineId;
    private String groupByKey;

}
