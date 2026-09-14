package org.prism.apm.parser.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.prism.apm.parser.constants.Constants.DELAY_SERVICE_LOCAL;

/**
 * @author john
 * @date 2024-10-23 15:10
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "delay.component.type",
        havingValue = DELAY_SERVICE_LOCAL,
        matchIfMissing = true)
public class SpanReprocessingSaveDelayQueueConsumer {


    @Autowired
    private DelayedQueueReprocessingConsumer delayedQueueConsumer;

    private ExecutorService executorService;


    @PostConstruct
    public void startConsumerThread() {
        // todo: make thread number configurable
        executorService = Executors.newFixedThreadPool(2);
        executorService.submit(delayedQueueConsumer);
        log.info("SpanReprocessingSaveDelayQueueConsumer started");

    }

    // Gracefully shut down the thread pool when the application stops
    @PreDestroy
    public void shutdownExecutorService() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown(); // Request shutdown
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow(); // Force shutdown after timeout
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }




}
