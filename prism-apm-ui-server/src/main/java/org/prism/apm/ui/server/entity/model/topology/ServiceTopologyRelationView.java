package org.prism.apm.ui.server.entity.model.topology;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ServiceTopologyRelationView {

    private LocalDateTime time;
    private String serviceName;
    private String hostName;
    private String targetName;
    private String systemType;
    private String component;
    private String spanKind;
    private String callerServiceName;
    private String callerHostName;
    private String callerTargetName;
    private String callerSystemType;
    private String callerComponent;
    private String callerSpanKind;
    private BigDecimal avgDuration;
    private BigDecimal maxDuration;
    private BigDecimal minDuration;
    private Long cnt;
    private Long errorNum;
    private BigDecimal p90Duration;
    private BigDecimal p95Duration;
    private BigDecimal p99Duration;

}
