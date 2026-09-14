package org.prism.apm.parser.scheduler;

import org.prism.apm.parser.cache.EhcacheService;
import org.prism.apm.parser.service.stats.StatsService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import org.prism.apm.parser.delay.DelayedSpan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.Resource;
import java.util.concurrent.DelayQueue;

import static org.prism.apm.parser.constants.Constants.CACHE_LOCAL;
import static org.prism.apm.parser.constants.Constants.DELAY_SERVICE_LOCAL;


@Data
@Service
@Slf4j
public class StatsScheduler {

    @Value("${cache.type:local}")
    private String cacheType;

    @Value("${delay.component.type:local}")
    private String delayHandlerType;


    @Autowired(required = false)
    private EhcacheService ehcacheService;


    @Resource
    @Qualifier("all-spans-delayqueue")
    private DelayQueue<DelayedSpan> delayQueue;

    @Resource
    @Qualifier("reprocessing-spans-delayqueue")
    private DelayQueue<DelayedSpan> reprocessingDelayQueue;

    @Resource
    private StatsService statsService;


    @Scheduled(cron = "0 * * * * ?")
    public void outputStats() {
        log.info("outputStats started");

        if (CACHE_LOCAL.equals(cacheType)) {
            log.info(ehcacheService.getStatistics());
        }

        if (DELAY_SERVICE_LOCAL.equals(delayHandlerType)) {
            StringBuilder sb = new StringBuilder();
            if (delayQueue != null) {
                sb.append("delay queue size:" + delayQueue.size() + ",");
            }

            if (reprocessingDelayQueue != null) {
                sb.append("reprocessing delay queue size:" + reprocessingDelayQueue.size());
            }
            log.info(sb.toString());
        }

        // output span stats
        log.info(statsService.getSpanStats());


        log.info("outputStats end");
    }
}
