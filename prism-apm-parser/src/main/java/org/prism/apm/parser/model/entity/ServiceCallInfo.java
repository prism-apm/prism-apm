package org.prism.apm.parser.model.entity;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author john
 * @date 2024-08-20 18:28
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceCallInfo {
    private Date time;

    private String serviceName;

    private String hostName;

    private String actionName;

    private String component;

    private Integer spanKind;

    private Date callerTime;

    private String callerServiceName;

    private String callerHostName;

    private String callerActionName;

    private String callerComponent;

    private Integer callerSpanKind;

    private Long duration;

    private Long error;
}