package org.prism.apm.ui.server.common;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import org.prism.apm.common.PostgresContainerInitializer;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@MybatisPlusTest
@TestPropertySource(properties = {
        "apollo.bootstrap.enabled = false",
        "logging.level.org.prism.apm.ui.server.mapper=TRACE",
        "mybatis.mapper-locations=classpath*:/mybatis/mapper/**/*.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MybatisTestBase extends PostgresContainerInitializer{

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> jdbcUrl(MybatisTestBase.class));
        registry.add("spring.datasource.username", () -> username(MybatisTestBase.class));
        registry.add("spring.datasource.password", () -> password(MybatisTestBase.class));
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:common/test-schema-init.sql");
    }

}
