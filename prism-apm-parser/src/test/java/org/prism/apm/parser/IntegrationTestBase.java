// file: src/test/java/org/prism/apm/parser/IntegrationTestBase.java
package org.prism.apm.parser;

import org.prism.apm.common.PostgresContainerInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;


@SpringBootTest
@Testcontainers
public abstract class IntegrationTestBase extends PostgresContainerInitializer {

    // Keep parser integration tests aligned with the shared production schema scripts
    // instead of maintaining a separate test-schema.sql copy.
    private static final List<String> SCHEMA_SCRIPT_NAMES = List.of(
            "10_create_service_call_objects.sql",
            "11_create_app_config_objects.sql"
    );

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> jdbcUrl(IntegrationTestBase.class));
        registry.add("spring.datasource.username", () -> username(IntegrationTestBase.class));
        registry.add("spring.datasource.password", () -> password(IntegrationTestBase.class));
        // In SpringBootTest, also disable data source replacement explicitly; it defaults to NONE, but being explicit is safer
        registry.add("spring.test.database.replace", () -> "none");

        // Ensure Spring boot executes DB initialization after the Datasource is created.
        // Otherwise, beans that read initial dta from DB during startup might throw an exception.
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> schemaScriptLocations(SCHEMA_SCRIPT_NAMES));
        // Fail fast when a shared schema script breaks, so schema drift is caught by integration tests.
        registry.add("spring.sql.init.continue-on-error", () -> "false");


    }

}
