package org.prism.apm.parser.scheduler;

import org.prism.apm.parser.config.FieldMatchRuleConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;


@Data
@Service
@Slf4j
public class ConfigScheduler {


    @Resource
    private FieldMatchRuleConfig fieldMatchRuleConfig;

    @Scheduled(cron = "0 * * * * ?")
    public void refreshConfig() {
        log.trace("config refresh started");

        fieldMatchRuleConfig.refresh();

        log.trace("config refresh end");
    }
}
