package org.prism.apm.parser.consumer;

import org.prism.apm.parser.config.DelayServiceConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data
@Slf4j
@Service
@NoArgsConstructor
@ConditionalOnProperty(name = "delay.component.type",
        havingValue = DELAY_SERVICE_LOCAL,
        matchIfMissing = true)
public class SpanSaveDelayQueueConsumer {

    @Autowired
    private DelayedQueueConsumer delayedQueueConsumer;

    @Autowired
    private DelayServiceConfig delayServiceConfig;

    private ExecutorService executorService;

    public SpanSaveDelayQueueConsumer(DelayedQueueConsumer delayedQueueConsumer, DelayServiceConfig delayServiceConfig) {
        this.delayedQueueConsumer = delayedQueueConsumer;
        this.delayServiceConfig = delayServiceConfig;
    }

    @PostConstruct
    public void startConsumerThread() {
        executorService = Executors.newFixedThreadPool(delayServiceConfig.getThreadNum());
        for (int i = 0; i < delayServiceConfig.getThreadNum(); i++) {
            executorService.submit(delayedQueueConsumer);
        }

        log.info("SpanSaveDelayQueueConsumer started");
    }

    // Gracefully shut down the thread pool when the application stops
    @PreDestroy
    public void shutdownExecutorService() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown(); // Request shutdown
            try {
                if (!executorService.awaitTermination(delayServiceConfig.getExecutorServiceShutdownTimeout(), TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow(); // Force shutdown after timeout
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }

}
