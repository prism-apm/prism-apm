package org.prism.apm.ui.server.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableRoutingRules {
    public enum RuleType {
        SIMPLE,       // ordinal=0
        COMPLEX   // ordinal=1
    }

    /**
     * Optional step size for routing rules
     * 1. The step size is related to the database design. The four enum values currently match the database continuous aggregate time_bucket.
     *  eg: time_bucket('5 minutes', time), => FIVE_MINUTE(Duration.ofMinutes(5).toMinutes())
     * 2. ONE_DAY has special handling, mainly to avoid timezone-related time display issues on the time axis. In practice, time is no longer important at this step.
     */
    @Getter
    @AllArgsConstructor
    public enum TableRoutingStep {
        ONE_MINUTE(Duration.ofMinutes(1)),//1
        FIVE_MINUTE(Duration.ofMinutes(5)),//5
        ONE_HOUR(Duration.ofHours(1)),//60
        ONE_DAY(Duration.ofDays(1));//1440
        private final Duration stepDuration;
    }

    private Long minSeconds;
    private Long maxSeconds;
    private String ruleType;
    private String targetTable;
    private String optimizedTable;
    private int chunkLagDays;
    private String tag;
    private int dataStep;

    public boolean matchesDuration(long durationSeconds) {
        boolean conditionMin = (minSeconds == null || durationSeconds >= minSeconds);
        boolean conditionMax = (maxSeconds == null || durationSeconds <= maxSeconds);
        return conditionMin && conditionMax;
    }
}

