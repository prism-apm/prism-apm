package org.prism.apm.common; // or the package path you expect

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@MapperScan("org.prism.apm.common.repository")
@MybatisTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class MybatisTestBase extends PostgresContainerInitializer {

    // 2. Add this static inner class
    // Its purpose is to provide a configuration entry point in a module without @SpringBootApplication,
    // for a module without , so Spring Boot auto-configuration is triggered correctly.
    @SpringBootConfiguration
    static class TestConfig {
    }

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> jdbcUrl(MybatisTestBase.class));
        registry.add("spring.datasource.username", () -> username(MybatisTestBase.class));
        registry.add("spring.datasource.password", () -> password(MybatisTestBase.class));
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:sql/test-schema.sql");

    }

}
