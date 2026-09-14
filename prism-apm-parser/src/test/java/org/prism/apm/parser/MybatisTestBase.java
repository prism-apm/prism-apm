package org.prism.apm.parser; // or the package path you expect

import org.prism.apm.common.PostgresContainerInitializer;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@MybatisTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class MybatisTestBase extends PostgresContainerInitializer {

    private static final List<String> SCHEMA_SCRIPT_NAMES = List.of(
            "10_create_service_call_objects.sql",
            "11_create_app_config_objects.sql"
    );

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> jdbcUrl(MybatisTestBase.class));
        registry.add("spring.datasource.username", () -> username(MybatisTestBase.class));
        registry.add("spring.datasource.password", () -> password(MybatisTestBase.class));
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> schemaScriptLocations(SCHEMA_SCRIPT_NAMES));
        registry.add("spring.sql.init.continue-on-error", () -> "false");

    }

}
