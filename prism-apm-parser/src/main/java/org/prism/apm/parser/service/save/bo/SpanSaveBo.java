package org.prism.apm.parser.service.save.bo;

import org.prism.apm.parser.model.span.SpanInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


/**
 * @author john
 * @date 2024-08-26 15:54
 */
@Component
public class SpanSaveBo {


    public boolean isTimeout(SpanInfo spanInfo, long currTime, long timeoutThreshold) {
        long diff = currTime - spanInfo.getServerArrivalTime();

        // if the processing of span times out, drop it
        return diff > timeoutThreshold;
    }


    public boolean isNoCallerSpan(SpanInfo spanInfo) {
        return StringUtils.isEmpty(spanInfo.getParentSpanId());
    }
}
