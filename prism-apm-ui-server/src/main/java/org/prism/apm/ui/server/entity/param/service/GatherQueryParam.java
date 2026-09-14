package org.prism.apm.ui.server.entity.param.service;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GatherQueryParam {

    private String serviceName;
    private String machineId;
    private String ifaceName;
    private String ifaceNameHashKey;

    private Long ifaceTotalCount;
    private BigDecimal ifaceAvgDuration;
    private BigDecimal ifaceTotalTime;//microseconds

    private String groupByKey;

}
