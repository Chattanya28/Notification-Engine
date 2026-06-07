package com.notification.gateway.config;

import com.notification.gateway.service.TemplateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;

@Configuration
public class ThymeleafConfig {

    @Bean
    public TemplateEngine stringTemplateEngine() {
        return TemplateService.createThymeleafTemplateEngine();
    }
}
