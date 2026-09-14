package prism.explorer.service;

import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Centralizes the JdbcTemplate instances for the prism_explorer and apm data sources
 * so callers do not need scattered injections.
 */
@Service
public class DataSourceService {

    private final JdbcTemplate prismExplorerJdbcTemplate;
    @Resource(name = "apmJdbcTemplate")
    private final JdbcTemplate apmJdbcTemplate;

    public DataSourceService(JdbcTemplate prismExplorerJdbcTemplate, JdbcTemplate apmJdbcTemplate) {
        this.prismExplorerJdbcTemplate = prismExplorerJdbcTemplate;
        this.apmJdbcTemplate = apmJdbcTemplate;
    }

    public JdbcTemplate getPrismExplorerJdbcTemplate() {
        return prismExplorerJdbcTemplate;
    }

    public JdbcTemplate getApmJdbcTemplate() {
        return apmJdbcTemplate;
    }
}
