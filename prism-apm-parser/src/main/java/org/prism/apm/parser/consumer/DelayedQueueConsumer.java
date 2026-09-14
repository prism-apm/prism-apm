package org.prism.apm.parser.consumer;

import org.prism.apm.parser.config.DelayServiceConfig;
import org.prism.apm.parser.delay.DelayedSpan;
import org.prism.apm.parser.service.save.SpanSaveService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import static org.prism.apm.parser.constants.Constants.DELAY_SERVICE_LOCAL;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@Service
@ConditionalOnProperty(name = "delay.component.type",
        havingValue = DELAY_SERVICE_LOCAL,
        matchIfMissing = true)
public class DelayedQueueConsumer implements Runnable {


    @Autowired
    @Qualifier("all-spans-delayqueue")
    private DelayQueue<DelayedSpan> delayQueue;

    @Autowired
    private SpanSaveService spanSaveService;

    @Autowired
    private DelayServiceConfig delayServiceConfig;

    @Value("${delayMillis:3000}")
    private long delayMillis;


    @Override
    public void run() {
        log.info("DelayedQueueConsumer started");


        while (true) {
            try {
                List<String> spanIdList = new ArrayList<>();


                // Get and process tasks from DelayQueue
                long startTime = System.currentTimeMillis(); // Record start time
                while (spanIdList.size() < delayServiceConfig.getBatchSize()) {

                    DelayedSpan delayedSpan = delayQueue.poll(delayServiceConfig.getTimeout(), TimeUnit.MILLISECONDS);
                    if (delayedSpan == null) {
                        break;
                    }

                    spanIdList.add(delayedSpan.getSpanId());
                }


                long endTime = System.currentTimeMillis();
                log.debug("delay queue take data interval:{}ms", (endTime - startTime));


                startTime = System.currentTimeMillis(); // Record start time
                // prepare data
                List<String> delayedProcessingSpanIds;

                if (!spanIdList.isEmpty()) {
                    try {
                        // handle data
                        delayedProcessingSpanIds = spanSaveService.handle(spanIdList, false);

                        // delay processing spans that have not get caller span yet
                        if (!delayedProcessingSpanIds.isEmpty()) {
                            for (String spanId : delayedProcessingSpanIds) {
                                log.debug("delayed processing spanId:{}", spanId);
                                // Delayed processing
                                delayQueue.put(new DelayedSpan(delayMillis, spanId));

                            }
                            log.debug("Number of spans being re-delayed for further processing:{}", delayedProcessingSpanIds.size());

                        }

                    } catch (Exception e) {
                        String spans = String.join("\n", spanIdList);
                        log.error("consumer error. spans size:{}. span ids{}", spanIdList.size(), spans, e);
                    }
                }


                //                    log.debug("delayed span size:" + delayedProcessingSpanIds.size());
                // Record end time
                endTime = System.currentTimeMillis();
                log.debug("delay queue processing time:{}", (endTime - startTime));

            } catch (InterruptedException ie) {
                log.error("consumer interrupted", ie);
                Thread.currentThread().interrupt();
                break; // Handle thread interruption
            } catch (Exception e) {
                log.error("consumer exception", e);
            }
        }

    }
}
