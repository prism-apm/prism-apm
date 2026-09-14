package org.prism.apm.parser.config;

import org.prism.apm.parser.delay.DelayedSpan;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.DelayQueue;

/**
 * @author john
 * @date 2024-10-23 15:03
 */
@Data
@Configuration
public class DelayQueueConfig {

    DelayQueue<DelayedSpan> allSpansDelayQueue;

    DelayQueue<DelayedSpan> reprocessingSpansDelayQueue;



    @Bean("all-spans-delayqueue")
    public DelayQueue<DelayedSpan> allSpansDelayQueue() {
        allSpansDelayQueue = new DelayQueue<>();
        return allSpansDelayQueue;
    }

    @Bean("reprocessing-spans-delayqueue")
    public DelayQueue<DelayedSpan> reprocessingSpansDelayQueue() {
        reprocessingSpansDelayQueue = new DelayQueue<>();
        return reprocessingSpansDelayQueue;
    }


}
