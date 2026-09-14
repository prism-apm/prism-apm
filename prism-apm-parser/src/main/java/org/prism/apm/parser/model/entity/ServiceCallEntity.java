package org.prism.apm.parser.model.entity;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * @author john
 * @date 2025-09-04 17:46
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ServiceCallEntity {
    private OffsetDateTime time;

    private String spanId;

    private String traceId;

    private String serviceName;

    private String hostName;

    private String actionName;

    private String targetName;

    private UUID targetNameId;

    private String remoteAddr;

    private String systemType;

    private String component;

    private Short spanKind;

    private OffsetDateTime callerTime;

    private String callerSpanId;

    private String callerServiceName;

    private String callerHostName;

    private String callerActionName;

    private String callerTargetName;

    private UUID callerTargetNameId;

    private String callerRemoteAddr;

    private String callerSystemType;

    private String callerComponent;

    private Short callerSpanKind;

    private Long cnt;

    private Long duration;

    private Long error;

    private OffsetDateTime createTime;

    private OffsetDateTime updateTime;
}