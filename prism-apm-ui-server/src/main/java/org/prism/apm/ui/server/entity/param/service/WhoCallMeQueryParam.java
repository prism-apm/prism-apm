package org.prism.apm.ui.server.entity.param.service;

import lombok.Data;

@Data
public class WhoCallMeQueryParam{

    private String serviceName;
    private String machineId;
    private String ifaceName;
    private String ifaceNameHashKey;

}
