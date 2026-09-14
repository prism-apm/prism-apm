package org.prism.apm.parser.model.dto;

import lombok.Data;

/**
 * @author john
 * @date 2024-05-08 16:43
 */
@Data
public class ServerSpanCacheDto {
    private String spanId;
    private String parentSpanId;
    private String hostName;
    private String serviceName;
    private String operationName;
}
