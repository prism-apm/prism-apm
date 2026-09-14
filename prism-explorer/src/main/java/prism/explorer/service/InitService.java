package prism.explorer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InitService {

    private static final Logger log = LoggerFactory.getLogger(InitService.class);

    private final SqlScriptService sqlScriptService;
    private final DataSourceService dataSourceService;

    public InitService(SqlScriptService sqlScriptService, DataSourceService dataSourceService) {
        this.sqlScriptService = sqlScriptService;
        this.dataSourceService = dataSourceService;
    }

    /**
     * Synchronously executes all initialization scripts under sql/init/.
     */
    public void runInit() {
        List<String> scripts = sqlScriptService.collectScripts("sql/init");
        log.info("Found {} initialization scripts: {}", scripts.size(), scripts);

        for (String scriptName : scripts) {
            log.info("Starting script: {}", scriptName);
            runSingleScript("sql/init/" + scriptName);
            log.info("Script executed successfully: {}", scriptName);
        }
    }

    private void runSingleScript(String path) {
        try {
            String sql = sqlScriptService.readScript(path);
            log.info("Executing script with apmJdbcTemplate: {}", path);
            sqlScriptService.executeSql(sql, dataSourceService.getApmJdbcTemplate());
            log.info("Script executed successfully: {}", path);
        } catch (Exception e) {
            log.error("Script execution error: {} - {}", path, e.toString());
            String summary = e.getClass().getSimpleName() + ": " + sqlScriptService.extractFirstLine(e.getMessage());
            throw new RuntimeException("Script execution failed: " + path + " - " + summary, e);
        }
    }
}
