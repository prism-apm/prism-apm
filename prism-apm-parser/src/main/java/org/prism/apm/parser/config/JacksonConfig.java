package org.prism.apm.parser.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // For better compatibility with fastjson behavior, configure:
        // During deserialization, do not throw an exception when the JSON string contains properties not present on the Java object.
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Other common configuration, depending on your needs:
        // - objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")); // Global date formatting
        // - objectMapper.registerModule(new JavaTimeModule()); // Support Java 8 date/time types, such as LocalDate and LocalDateTime

        return objectMapper;
    }
}
