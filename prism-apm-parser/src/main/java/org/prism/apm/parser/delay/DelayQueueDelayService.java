package org.prism.apm.parser.delay;

import org.prism.apm.parser.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.DelayQueue;

import static org.prism.apm.parser.constants.Constants.DELAY_SERVICE_LOCAL;

/**
 * @author john
 * @date 2024-10-23 15:00
 */
@Service
@NoArgsConstructor
@AllArgsConstructor
@ConditionalOnProperty(name = "delay.component.type",
        havingValue = DELAY_SERVICE_LOCAL,
        matchIfMissing = true)
public class DelayQueueDelayService implements DelayService {


    @Autowired
    @Qualifier("all-spans-delayqueue")
    private DelayQueue<DelayedSpan> delayQueue;

    @Autowired
    @Qualifier("reprocessing-spans-delayqueue")
    private DelayQueue<DelayedSpan> reprocessingDelayQueue;

    @Override
    public void processSpan(String spanId, String processType) {
        if (Constants.SPAN_PROCESS_TYPE_FIRST.equals(processType)) {
            delayQueue.put(new DelayedSpan(0, spanId));
        } else if (Constants.SPAN_PROCESS_TYPE_AGAIN.equals(processType)) {
            reprocessingDelayQueue.put(new DelayedSpan(0, spanId));
        } else {
            throw new RuntimeException("unknown processtype:" + processType);

        }

    }
}
