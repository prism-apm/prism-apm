package org.prism.apm.common.sql;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Testcontainers
class TimescaleDdlInitializationTest {

    private static final DockerImageName TIMESCALE_IMAGE = DockerImageName
            .parse("timescale/timescaledb-ha:pg16.11-ts2.23.1")
            .asCompatibleSubstituteFor("postgres");

    private static final List<String> SQL_SCRIPT_NAMES = List.of(
            "10_create_service_call_objects.sql",
            "11_create_app_config_objects.sql",
            "20_init-common-permission.sql",
            "21_init-common-replacer-rule.sql",
            "22_init-common-table-routing-rules.sql",
            "30_warmup_data.sql"
    );

    @Container
    private static final PostgreSQLContainer<?> DATABASE_CONTAINER = new PostgreSQLContainer<>(TIMESCALE_IMAGE)
            .withDatabaseName("apm")
            .withUsername("testuser")
            .withPassword("testpass");

    /**
     * Verifies if the SQL scripts execute properly
     * @throws SQLException
     */
    @Test
    void shouldInitializeApmSqlScriptsInOrder() throws SQLException {
        // The SQL scripts are shared at the parent prism-apm/sql level, not copied into this module.
        Path sqlDirectory = findSqlDirectory();

        assertRequiredScriptsExist(sqlDirectory);

        // A fresh TimescaleDB container makes this test catch non-idempotent DDL problems cleanly.
        try (Connection connection = DriverManager.getConnection(
                DATABASE_CONTAINER.getJdbcUrl(),
                DATABASE_CONTAINER.getUsername(),
                DATABASE_CONTAINER.getPassword())) {

            for (String scriptName : SQL_SCRIPT_NAMES) {
                executeScript(connection, sqlDirectory.resolve(scriptName));
            }
        }
    }

    private static Path findSqlDirectory() {
        Path current = Path.of("").toAbsolutePath();
        Path parent = current.getParent();
        if (parent == null) {
            fail("Cannot resolve prism-apm parent directory from working directory: %s", current);
        }

        return parent.resolve("sql");
    }

    private static void assertRequiredScriptsExist(Path sqlDirectory) {
        assertThat(sqlDirectory)
                .as("shared SQL directory")
                .isDirectory();

        for (String scriptName : SQL_SCRIPT_NAMES) {
            assertThat(sqlDirectory.resolve(scriptName))
                    .as("required SQL script %s", scriptName)
                    .isRegularFile();
        }
    }

    private static void executeScript(Connection connection, Path scriptPath) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(scriptPath, StandardCharsets.UTF_8));
        } catch (IOException | SQLException e) {
            fail("Failed to execute SQL script: %s".formatted(scriptPath), e);
        }
    }

}
