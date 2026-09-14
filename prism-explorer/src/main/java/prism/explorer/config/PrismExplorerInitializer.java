package prism.explorer.config;

import prism.explorer.dao.PrismExplorerDatabaseDao;
import prism.explorer.service.InitService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PrismExplorerInitializer {

    private static final Logger log = LoggerFactory.getLogger(PrismExplorerInitializer.class);

    private final PrismExplorerDatabaseDao explorerDao;
    private final InitService initService;

    public PrismExplorerInitializer(PrismExplorerDatabaseDao explorerDao, InitService initService) {
        this.explorerDao = explorerDao;
        this.initService = initService;
    }

    @PostConstruct
    public void init() {
        explorerDao.getJdbcTemplate().execute("""
            CREATE TABLE IF NOT EXISTS prism_explorer_config (
                key         TEXT PRIMARY KEY,
                value       TEXT NOT NULL DEFAULT '',
                updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
            )
        """);

        explorerDao.getJdbcTemplate().execute("""
            CREATE TABLE IF NOT EXISTS mock_data_usage (
                id              SERIAL PRIMARY KEY,
                script_name     TEXT NOT NULL,
                target_date     TEXT NOT NULL,
                created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                UNIQUE (script_name, target_date)
            )
        """);

        log.info("Initialization status: {}", explorerDao.isInitDone() ? "completed" : "not completed");

        if (!explorerDao.isInitDone()) {
            log.info("Not initialized; starting initialization scripts...");
            try {
                initService.runInit();
                explorerDao.markInitDone();
                log.info("apm database initialization completed");
            } catch (Exception e) {
                log.error("apm database initialization failed", e);
                throw new RuntimeException("apm database initialization failed; application cannot start", e);
            }
        } else {
            log.info("apm database is already initialized; skipping");
        }
    }
}
