package org.prism.apm.ui.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    /**
     * The purpose is to allow RestTemplate configuration later
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
