package prism.explorer.service;

import prism.explorer.dao.PrismExplorerDatabaseDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Business logic for mock data generation and cleanup, separated from the Controller HTTP layer.
 *
 * <p>All methods run synchronously and are invoked by the Controller in an async thread.</p>
 */
@Service
public class MockDataService {

    private static final Logger log = LoggerFactory.getLogger(MockDataService.class);

    private final SqlScriptService sqlScriptService;
    private final DataSourceService dataSourceService;
    private final PrismExplorerDatabaseDao explorerDao;

    public MockDataService(SqlScriptService sqlScriptService,
                           DataSourceService dataSourceService,
                           PrismExplorerDatabaseDao explorerDao) {
        this.sqlScriptService = sqlScriptService;
        this.dataSourceService = dataSourceService;
        this.explorerDao = explorerDao;
    }

    /**
     * Generates mock data for the specified date range.
     *
     * @param startDate start date (yyyy-MM-dd)
     * @param endDate   end date (yyyy-MM-dd)
     * @param state     task state callback for progress and results
     */
    public void executeData(String startDate, String endDate, TaskState state) {
        List<String> sampleScripts = sqlScriptService.collectScripts("sql/data/samples");
        List<String> postScripts = sqlScriptService.collectScripts("sql/data/post");
        log.info("Task {}: sample scripts {}, post scripts {}", state.taskId, sampleScripts, postScripts);

        try {
            state.lastFailStage = "Data generation";
            state.update("Sample scripts: " + sampleScripts.size() + ", post scripts: " + postScripts.size(), false);

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            List<LocalDate> targetDates = new ArrayList<>();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                targetDates.add(d);
            }

            int totalExecuted = 0;
            List<String> dailyLogs = new ArrayList<>();
            for (LocalDate targetDate : targetDates) {
                String dateStr = targetDate.toString();
                String yyyyMMdd = dateStr.replace("-", "");

                // Query scripts used on the target date and the previous day.
                List<String> usedScripts = new ArrayList<>();
                usedScripts.addAll(explorerDao.getUsedScripts(yyyyMMdd));
                String prevDate = targetDate.minusDays(1).toString().replace("-", "");
                usedScripts.addAll(explorerDao.getUsedScripts(prevDate));

                List<String> available = new ArrayList<>(sampleScripts);
                available.removeAll(usedScripts);

                if (available.isEmpty()) {
                    log.info("Task {}: skipped date {}; all scripts were used on the target date and previous day",
                            state.taskId, dateStr);
                    dailyLogs.add(dateStr + " skipped (no scripts available)");
                    continue;
                }

                // Pick one script randomly.
                String chosen = available.get(ThreadLocalRandom.current().nextInt(available.size()));
                log.info("Task {}: date {} selected script {}", state.taskId, dateStr, chosen);
                state.update("Date " + dateStr + " selected " + chosen + "(" + (totalExecuted + 1) + "/" + targetDates.size() + ")", false);

                // Read the script.
                String sql = sqlScriptService.readScript("sql/data/samples/" + chosen);

                // For one day: shift dates, apply perturbation, and execute.
                LocalDate baseDate = sqlScriptService.extractBaseDate(sql);
                LocalDate utcStart = targetDate.minusDays(1);
                long dayOffset = ChronoUnit.DAYS.between(baseDate, utcStart);
                String transformed = sqlScriptService.offsetDates(sql, dayOffset);
                transformed = sqlScriptService.perturbDuration(transformed, new Random());
                transformed = sqlScriptService.perturbError(transformed, new Random());
                sqlScriptService.executeSql(transformed, dataSourceService.getApmJdbcTemplate());

                // Record script usage.
                explorerDao.recordScriptUsage(chosen, yyyyMMdd);

                dailyLogs.add(dateStr + " → " + chosen);
                totalExecuted++;
                Thread.sleep(1000);
            }

            // Execute post scripts.
            state.lastFailStage = "Post scripts";
            for (String postScript : postScripts) {
                log.info("Task {}: executing post script {}", state.taskId, postScript);
                state.update("Post script: " + postScript, false);
                String sql = sqlScriptService.readScript("sql/data/post/" + postScript);
                sqlScriptService.executeSql(sql, dataSourceService.getApmJdbcTemplate());
            }

            // Mark mock data as generated.
            explorerDao.markMockDataGenerated();

            // Build a user-friendly result.
            StringBuilder sb = new StringBuilder();
            sb.append("✅ Complete! Processed ").append(totalExecuted).append(" days of data\n");
            sb.append("Date range: ").append(startDate).append(" ~ ").append(endDate).append("\n");
            sb.append("┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄\n");
            for (String logEntry : dailyLogs) {
                sb.append(logEntry).append("\n");
            }
            if (!postScripts.isEmpty()) {
                sb.append("┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄\n");
                sb.append("Post scripts executed\n");
            }

            state.done = true;
            state.message = sb.toString();
            state.messageIsLong = true;
            log.info("Task {}: completed", state.taskId);
        } catch (Exception e) {
            log.error("Task {} failed", state.taskId, e);
            state.error = "Execution error";
            state.message = "❌ Execution failed: " + state.lastFailStage + " stage error";
            state.done = true;
        }
    }

    /**
     * Clears all data.
     *
     * @param state task state callback for progress and results
     */
    public void executeClear(TaskState state) {
        List<String> scripts = sqlScriptService.collectScripts("sql/clear");
        log.info("Cleanup task {}: found {} scripts: {}", state.taskId, scripts.size(), scripts);

        try {
            state.lastFailStage = "Data cleanup";
            state.totalScripts = scripts.size();
            state.update("Total: " + scripts.size() + " scripts to execute", false);

            for (int i = 0; i < scripts.size(); i++) {
                String scriptName = scripts.get(i);
                state.currentScriptIndex = i + 1;
                log.info("Cleanup task {}: executing script ({}/{}) {}", state.taskId, i + 1, scripts.size(), scriptName);
                state.update("Running (" + (i + 1) + "/" + scripts.size() + "): " + scriptName, false);

                String sql = sqlScriptService.readScript("sql/clear/" + scriptName);
                if (scriptName.contains("prism_explorer")) {
                    sqlScriptService.executeSql(sql, dataSourceService.getPrismExplorerJdbcTemplate());
                } else {
                    sqlScriptService.executeSql(sql, dataSourceService.getApmJdbcTemplate());
                }

                Thread.sleep(500);
            }

            // Reset the mock-data generated marker.
            explorerDao.resetMockDataGenerated();

            state.done = true;
            log.info("Cleanup task {}: completed", state.taskId);
            state.message = "✅ Cleanup complete! Executed " + scripts.size() + " scripts";
        } catch (Exception e) {
            log.error("Cleanup task {} failed", state.taskId, e);
            state.error = "Cleanup error";
            state.message = "❌ Cleanup failed: " + state.lastFailStage + " stage error";
            state.done = true;
        }
    }

    /**
     * Simple task state used by Controller polling.
     */
    public static class TaskState {
        final String taskId;
        boolean done;
        String error;
        String message;
        boolean messageIsLong;
        String lastFailStage;
        int totalScripts;
        int currentScriptIndex;

        public TaskState(String taskId) {
            this.taskId = taskId;
        }

        public void update(String message, boolean done) {
            this.message = message;
            this.done = done;
        }

        public Map<String, Object> snapshot() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", taskId);
            m.put("done", done);
            m.put("message", message);
            m.put("totalScripts", totalScripts);
            m.put("currentScriptIndex", currentScriptIndex);
            if (error != null) m.put("error", error);
            return m;
        }
    }
}
