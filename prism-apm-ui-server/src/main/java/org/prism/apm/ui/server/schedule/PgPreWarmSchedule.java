package org.prism.apm.ui.server.schedule;

import org.prism.apm.ui.server.mapper.schedule.ApmScheduleTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL database prewarm scheduled task scheduler
 * <p>
 * This class periodically executes database functions to prewarm PostgreSQL,
 * improving query performance. Execution frequency is controlled by the configured cron expression,
 * and the database function name can be adjusted dynamically.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PgPreWarmSchedule implements SchedulingConfigurer {
    /**
     * Configuration key for the scheduled task switch
     */
    private static final String DB_TASK_ENABLED_KEY = "apm.schedule.prewarm-enabled";
    
    /**
     * Default scheduled task switch: disabled
     */
    private static final boolean DB_TASK_ENABLED_DEFAULT = false;

    /**
     * Configuration key for the database function name
     */
    private static final String DB_FUNCTION_NAME_KEY = "apm.schedule.prewarm-function";
    
    /**
     * Default database function name to execute
     */
    private static final String DB_FUNCTION_NAME_DEFAULT = "prewarm_data";
    
    /**
     * Configuration key for the cron expression
     */
    private static final String DB_CRON_KEY = "apm.schedule.prewarm-cron";
    
    /**
     * Default cron expression: run every 10 minutes
     */
    private static final String DB_CRON_DEFAULT = "0 0/10 * * * ?";

    /**
     * Database task mapper used to execute database functions
     */
    private final ApmScheduleTaskMapper apmScheduleTaskMapper;
    
    /**
     * Spring environment used to read configuration
     */
    private final Environment environment;

    /**
     * Configure scheduled task
     * <p>
     * This method implements SchedulingConfigurer and registers the scheduled task.
     * It schedules executeDbFunction according to the configured cron expression.
     * If the configured cron expression is invalid, it falls back to the default cron expression.
     * 
     * @param taskRegistrar Scheduled task registrar
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(this::executeDbFunction, triggerContext -> {
            // Get the cron expression from configuration; use the default value if not configured
            String cron = environment.getProperty(DB_CRON_KEY, DB_CRON_DEFAULT);
            try {
                // Create CronTrigger and return the next execution time
                return new CronTrigger(cron).nextExecution(triggerContext);
            } catch (IllegalArgumentException ex) {
                // If the cron expression is invalid, log the error and use the default cron expression
                log.error("PostgreSQL prewarm task Invalid cron expression: {}, fallback to default: {}", cron, DB_CRON_DEFAULT, ex);
                return new CronTrigger(DB_CRON_DEFAULT).nextExecution(triggerContext);
            }
        });
    }

    /**
     * Execute database function
     * <p>
     * This method gets the database function name to execute from configuration,
     * then calls ApmScheduleTaskMapper to execute the function.
     * The execution result is recorded in logs.
     */
    public void executeDbFunction() {
        boolean taskEnabled = Boolean.parseBoolean(
                environment.getProperty(DB_TASK_ENABLED_KEY, String.valueOf(DB_TASK_ENABLED_DEFAULT))
        );
        if (!taskEnabled) {
            log.debug("Skip PostgreSQL prewarm task because switch is off. config: {}=false", DB_TASK_ENABLED_KEY);
            return;
        }

        // Get the database function name from configuration; use the default value if not configured
        String dbFunctionName = environment.getProperty(DB_FUNCTION_NAME_KEY, DB_FUNCTION_NAME_DEFAULT);
        dbFunctionName = String.format("%s()", dbFunctionName);
        log.info("Start PostgreSQL prewarm task. function: {}", dbFunctionName);
        try {
            // Call the mapper to execute the database function
            String functionResult = apmScheduleTaskMapper.executeFunction(dbFunctionName);
            log.info("PostgreSQL prewarm task succeeded. function: {}, result: {}", dbFunctionName, functionResult);
        } catch (Exception ex) {
            // Log execution failure
            log.error("PostgreSQL prewarm task failed. function: {}", dbFunctionName, ex);
        }
    }

}
