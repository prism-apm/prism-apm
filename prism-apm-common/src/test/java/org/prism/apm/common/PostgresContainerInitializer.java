package org.prism.apm.common;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Shared TimescaleDB test utilities.
 * Each test base class asks for a container by its own class key, so tests that initialize
 * incompatible schemas do not accidentally share PostgreSQL cluster-level objects such as roles.
 */
public abstract class PostgresContainerInitializer {

    private static final DockerImageName TIMESCALE_IMAGE = DockerImageName
            .parse("timescale/timescaledb-ha:pg16.11-ts2.23.1")
            .asCompatibleSubstituteFor("postgres");

    private static final Map<Class<?>, PostgreSQLContainer<?>> DATABASE_CONTAINERS = new ConcurrentHashMap<>();

    protected static PostgreSQLContainer<?> databaseContainer(Class<?> owner) {
        Objects.requireNonNull(owner, "owner");
        return DATABASE_CONTAINERS.computeIfAbsent(owner, PostgresContainerInitializer::startContainer);
    }

    private static PostgreSQLContainer<?> startContainer(Class<?> owner) {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(TIMESCALE_IMAGE)
                .withDatabaseName("apm")
                .withUsername("testuser")
                .withPassword("testpass");

        container.start();
        return container;
    }

    protected static String jdbcUrl(Class<?> owner) {
        return databaseContainer(owner).getJdbcUrl();
    }

    protected static String username(Class<?> owner) {
        return databaseContainer(owner).getUsername();
    }

    protected static String password(Class<?> owner) {
        return databaseContainer(owner).getPassword();
    }

    protected static Path findSharedSqlDirectory() {
        Path current = Path.of("").toAbsolutePath();
        Path parent = current.getParent();
        if (parent != null) {
            Path parentCandidate = parent.resolve("sql");
            if (Files.isDirectory(parentCandidate)) {
                return parentCandidate;
            }
        }

        Path currentCandidate = current.resolve("sql");
        if (Files.isDirectory(currentCandidate)) {
            return currentCandidate;
        }

        throw new IllegalStateException("Cannot find prism-apm/sql from working directory: " + current);
    }

    protected static String schemaScriptLocations(List<String> scriptNames) {
        Path sqlDirectory = findSharedSqlDirectory();
        for (String scriptName : scriptNames) {
            Path script = sqlDirectory.resolve(scriptName);
            if (!Files.isRegularFile(script)) {
                throw new IllegalStateException("Cannot find required SQL script: " + script);
            }
        }

        return scriptNames.stream()
                .map(sqlDirectory::resolve)
                .map(path -> path.toUri().toString())
                .collect(Collectors.joining(","));
    }
}
