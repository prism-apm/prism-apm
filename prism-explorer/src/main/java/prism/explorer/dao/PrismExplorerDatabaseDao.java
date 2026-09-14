package prism.explorer.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import prism.explorer.service.DataSourceService;

import java.util.List;
import java.util.Map;

/**
 * Data access layer for the prism_explorer database.
 * Handles CRUD operations for the prism_explorer_config and mock_data_usage tables.
 */
@Repository
public class PrismExplorerDatabaseDao {

    private final JdbcTemplate jdbcTemplate;

    public PrismExplorerDatabaseDao(DataSourceService dataSourceService) {
        this.jdbcTemplate = dataSourceService.getPrismExplorerJdbcTemplate();
    }

    /** Exposes JdbcTemplate for PrismExplorerInitializer to execute DDL. */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /** Checks whether initialization is complete. */
    public boolean isInitDone() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT 1 FROM prism_explorer_config WHERE key = 'init_done' AND value = 'true'");
        return !rows.isEmpty();
    }

    /** Marks initialization as complete. */
    public void markInitDone() {
        jdbcTemplate.update(
                "INSERT INTO prism_explorer_config (key, value) VALUES ('init_done', 'true') ON CONFLICT (key) DO UPDATE SET value = 'true', updated_at = now()");
    }

    /** Checks whether mock data has already been generated. */
    public boolean isMockDataGenerated() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT 1 FROM prism_explorer_config WHERE key = 'mock_data_generated' AND value = 'true'");
        return !rows.isEmpty();
    }

    /** Marks mock data as generated. */
    public void markMockDataGenerated() {
        jdbcTemplate.update(
                "INSERT INTO prism_explorer_config (key, value) VALUES ('mock_data_generated', 'true') ON CONFLICT (key) DO UPDATE SET value = 'true', updated_at = now()");
    }

    /** Resets the mock-data generated marker. */
    public void resetMockDataGenerated() {
        jdbcTemplate.update(
                "DELETE FROM prism_explorer_config WHERE key = 'mock_data_generated'");
    }

    /** Queries script names used on the specified date. */
    public List<String> getUsedScripts(String yyyyMMdd) {
        return jdbcTemplate.queryForList(
                "SELECT script_name FROM mock_data_usage WHERE target_date = ?", String.class, yyyyMMdd);
    }

    /** Records the script used on a given date. */
    public void recordScriptUsage(String scriptName, String yyyyMMdd) {
        jdbcTemplate.update(
                "INSERT INTO mock_data_usage (script_name, target_date) VALUES (?, ?) ON CONFLICT DO NOTHING",
                scriptName, yyyyMMdd);
    }
}
