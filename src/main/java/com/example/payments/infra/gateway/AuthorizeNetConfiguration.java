package com.example.payments.infra.gateway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthorizeNetProperties.class)
public class AuthorizeNetConfiguration {
}
