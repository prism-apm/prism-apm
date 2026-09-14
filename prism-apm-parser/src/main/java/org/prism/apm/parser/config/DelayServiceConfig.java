package org.prism.apm.parser.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author john
 * @date 2024-11-20 16:58
 */
@Configuration
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DelayServiceConfig {

    @Value("${delay.component.local.thread_num:4}")
    private int threadNum;

    @Value("${delay.component.local.batch_size:10}")
    private int batchSize;

    @Value("${delay.component.local.batch_timeout_ms:1000}")
    private long timeout;

    @Value("${executor.service.shutdown.timeout_ms:10000}")
    private long executorServiceShutdownTimeout;

}
