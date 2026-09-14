package org.prism.apm.parser.model.span;

import lombok.Data;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * @author peter
 * @create 2019-07-06
 * todo: Can these Long wrapper types be changed to primitive long?
 */
@Data
@ToString
public class SpanInfo {
    private String hostName;
    private Short spanKind;
    private String component;
    private Long duration;
    private String serviceName;
    private String actionName;
    private String spanId;
    private String traceId;
    private Long error = 0L;
    private Long time = 0L;
    private String parentSpanId;

    private String remoteAddr;
    private String targetName;
    private String systemType;

    // time of span arrive this processing server
    private long serverArrivalTime;

//    private Map<String, String> extraData = new HashMap<>();

    public boolean checkRequiredFields() {
        return time == 0 || StringUtils.isEmpty(traceId) || StringUtils.isEmpty(spanId)
                || StringUtils.isEmpty(serviceName) || hostName == null
                || actionName == null || StringUtils.isEmpty(targetName)
                || remoteAddr == null || systemType == null
                || StringUtils.isEmpty(component) || spanKind == null
                || duration == null || error == null
                ;
    }

}
