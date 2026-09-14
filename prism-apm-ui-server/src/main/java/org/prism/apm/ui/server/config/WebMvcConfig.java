package org.prism.apm.ui.server.config;

import org.prism.apm.ui.server.interceptor.DateValidateInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final DateValidateInterceptor dateValidateInterceptor;

    public WebMvcConfig(DateValidateInterceptor dateValidateInterceptor) {
        this.dateValidateInterceptor = dateValidateInterceptor;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dateValidateInterceptor)
                .addPathPatterns("/api/apm/common/**", "/api/apm/service/**");
    }
}
