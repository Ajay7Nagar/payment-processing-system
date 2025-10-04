package com.example.payments.infra.gateway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(AuthorizeNetProperties.class)
public class AuthorizeNetConfiguration {

    @Bean
    RestTemplate authorizeNetRestTemplate() {
        return new RestTemplate();
    }
}
