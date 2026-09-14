package prism.explorer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @Primary
    public DataSource prismExplorerDataSource() {
        ensureDatabases();
        return DataSourceBuilder.create()
                .url("jdbc:postgresql://timescaledb:5432/prism_explorer")
                .username("postgres")
                .password("postgres")
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    public DataSource apmDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:postgresql://timescaledb:5432/apm?reWriteBatchedInserts=false")
                .username("postgres")
                .password("postgres")
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    @Primary
    public JdbcTemplate prismExplorerJdbcTemplate(@Qualifier("prismExplorerDataSource") DataSource prismExplorerDataSource) {
        return new JdbcTemplate(prismExplorerDataSource);
    }

    @Bean
    public JdbcTemplate apmJdbcTemplate(@Qualifier("apmDataSource") DataSource apmDataSource) {
        return new JdbcTemplate(apmDataSource);
    }

    private void ensureDatabases() {
        String[] dbNames = {"prism_explorer", "apm"};
        for (String dbName : dbNames) {
            ensureDatabase(dbName);
        }
    }

    private void ensureDatabase(String dbName) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://timescaledb:5432/postgres", "postgres", "postgres");
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")) {
                if (rs.next()) {
                    log.info("Database {} already exists", dbName);
                    return;
                }
            }

            stmt.execute("CREATE DATABASE " + dbName);
            log.info("Created database {}", dbName);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create database " + dbName + ": " + e.getMessage(), e);
        }
    }
}
