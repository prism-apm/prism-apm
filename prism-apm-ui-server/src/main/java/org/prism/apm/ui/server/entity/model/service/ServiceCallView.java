package org.prism.apm.ui.server.entity.model.service;

import org.prism.apm.ui.server.constant.SpanConstants;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ServiceCallView {

    private LocalDateTime timeb;
    private String serviceName;
    private String actionName;
    private String targetName;
    private String systemType;
    private String callerServiceName;
    private String callerTargetName;
    private BigDecimal totalTime;
    private Long totalCount;
    private Long totalError;
    private BigDecimal avgDuration;//micro seconds
    private BigDecimal max;
    private BigDecimal min;
    private BigDecimal p90;
    private BigDecimal p95;
    private BigDecimal p99;
    
    public static ServiceCallView createInstanceWithDefaultValue() {
        ServiceCallView instance = new ServiceCallView();
        instance.setSystemType(SpanConstants.DEFAULT_STRING_VALUE_WHEN_NO_DATA);
        instance.setTotalTime(BigDecimal.ZERO);
        instance.setTotalCount(0L);
        instance.setTotalError(0L);
        instance.setAvgDuration(BigDecimal.ZERO);
        instance.setMax(BigDecimal.ZERO);
        instance.setMin(BigDecimal.ZERO);
        instance.setP90(BigDecimal.ZERO);
        instance.setP95(BigDecimal.ZERO);
        instance.setP99(BigDecimal.ZERO);
        return instance;
    }

}
