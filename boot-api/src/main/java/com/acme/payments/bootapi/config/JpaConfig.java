package com.acme.payments.bootapi.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(name = "app.jpa.enabled", havingValue = "true", matchIfMissing = true)
@EntityScan(basePackages = {
        "com.acme.payments.adapters.out.db.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.acme.payments.adapters.out.db.repo"
})
public class JpaConfig {
}


