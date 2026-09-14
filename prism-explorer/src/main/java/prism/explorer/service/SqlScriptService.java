package prism.explorer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Shared utility methods for executing SQL scripts.
 */
@Service
public class SqlScriptService {

    private static final Logger log = LoggerFactory.getLogger(SqlScriptService.class);

    private final DataSourceService dataSourceService;

    public SqlScriptService(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    /** Reads a SQL file from the classpath as a string. */
    public String readScript(String path) {
        try {
            var resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read script: " + path, e);
        }
    }

    /** Scans all .sql files in the specified classpath directory. */
    public List<String> collectScripts(String subDir) {
        try {
            var resource = new ClassPathResource(subDir);
            var url = resource.getURL();
            try (var files = java.nio.file.Files.list(java.nio.file.Path.of(url.toURI()))) {
                return files
                        .map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".sql"))
                        .sorted()
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to scan directory: " + subDir, e);
        }
    }

    /**
     * Executes SQL against the specified data source. Multi-statement scripts use one connection
     * to keep BEGIN/COMMIT behavior consistent; CALL statements are handled separately and retried
     * automatically on concurrent refresh errors.
     */
    public void executeSql(String sql, JdbcTemplate jdbcTemplate) throws Exception {
        if (sql.contains("CALL ")) {
            String[] parts = sql.split("(?=\\bCALL\\s+)");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("CALL ")) {
                    executeWithRetry(trimmed, jdbcTemplate);
                } else {
                    try (Connection conn = jdbcTemplate.getDataSource().getConnection();
                         Statement stmt = conn.createStatement()) {
                        conn.setAutoCommit(true);
                        stmt.execute(trimmed);
                    }
                }
            }
        } else {
            try (Connection conn = jdbcTemplate.getDataSource().getConnection();
                 Statement stmt = conn.createStatement()) {
                conn.setAutoCommit(true);
                stmt.execute(sql);
            }
        }
    }

    /** Executes a single CALL statement and retries up to three times on concurrent refresh errors. */
    private void executeWithRetry(String callSql, JdbcTemplate jdbcTemplate) throws Exception {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection conn = jdbcTemplate.getDataSource().getConnection();
                 Statement stmt = conn.createStatement()) {
                conn.setAutoCommit(true);
                stmt.execute(callSql);
                return;
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("concurrent refresh") && attempt < maxRetries) {
                    log.warn("CALL hit concurrent refresh; retrying in {} seconds ({}/{})", attempt, attempt, maxRetries);
                    Thread.sleep(3000L * attempt);
                } else {
                    throw e;
                }
            }
        }
    }

    /** Returns the first line of an exception message to avoid leaking large SQL contents. */
    public String extractFirstLine(String msg) {
        if (msg == null || msg.isEmpty()) return "Unknown error";
        int idx = msg.indexOf('\n');
        return idx > 0 ? msg.substring(0, idx) : msg;
    }

    /** Extracts the base date from the first TIMESTAMPTZ literal in the SQL text. */
    public LocalDate extractBaseDate(String sql) {
        Matcher m = BASE_DATE_PATTERN.matcher(sql);
        if (m.find()) {
            return LocalDate.parse(m.group(1));
        }
        throw new RuntimeException("Unable to parse base date from script");
    }

    /** Shifts the date portion of all TIMESTAMPTZ literals by the specified number of days, skipping the 1970-01-01 marker. */
    public String offsetDates(String sql, long days) {
        Matcher m = TIMESTAMPTZ_PATTERN.matcher(sql);
        StringBuilder sb = new StringBuilder(sql.length() + 32);
        while (m.find()) {
            String dateStr = m.group(1);
            if ("1970-01-01".equals(dateStr)) {
                m.appendReplacement(sb, m.group(0));
                continue;
            }
            LocalDate d = LocalDate.parse(dateStr);
            LocalDate shifted = d.plusDays(days);
            m.appendReplacement(sb, "'" + shifted + m.group(2));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Perturbs all service_call durations in SQL by -30% to -10% or 10% to 30%, rounded up with a minimum of 1. */
    public String perturbDuration(String sql, Random rng) {
        Matcher m = DURATION_PATTERN.matcher(sql);
        StringBuilder sb = new StringBuilder(sql.length() + 32);
        while (m.find()) {
            int val = Integer.parseInt(m.group(2));
            boolean negative = rng.nextBoolean();
            double factor;
            if (negative) {
                factor = 0.7 + rng.nextDouble() * 0.2; // 0.7 ~ 0.9 (-30% ~ -10%)
            } else {
                factor = 1.1 + rng.nextDouble() * 0.2; // 1.1 ~ 1.3 (10% ~ 30%)
            }
            int perturbed = (int) Math.ceil(val * factor);
            if (perturbed < 1) perturbed = 1;
            m.appendReplacement(sb, m.group(1) + perturbed + m.group(3));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Changes error from 0 to 1 with a 0.1% probability. */
    public String perturbError(String sql, Random rng) {
        Matcher m = ERROR_PATTERN.matcher(sql);
        StringBuilder sb = new StringBuilder(sql.length() + 32);
        while (m.find()) {
            if (rng.nextInt(1000) == 0) {
                m.appendReplacement(sb, m.group(1) + "2" + m.group(2));
            } else {
                m.appendReplacement(sb, m.group(0));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern BASE_DATE_PATTERN = Pattern.compile("'(\\d{4}-\\d{2}-\\d{2})T\\d{2}:\\d{2}:\\d{2}");
    private static final Pattern TIMESTAMPTZ_PATTERN = Pattern.compile("'(\\d{4}-\\d{2}-\\d{2})(T\\d{2}:\\d{2}:\\d{2}[^']*'::TIMESTAMPTZ)");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(,\\s*)(\\d{1,10})(,\\s*[01],\\s*'\\d{4}-\\d{2}-\\d{2}T)");
    private static final Pattern ERROR_PATTERN = Pattern.compile("(,\\s*\\d{1,10},\\s*)0(,\\s*'\\d{4}-\\d{2}-\\d{2}T)");
}
