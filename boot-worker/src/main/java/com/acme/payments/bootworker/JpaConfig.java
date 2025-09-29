package com.acme.payments.bootworker;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
        "com.acme.payments.adapters.out.db.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.acme.payments.adapters.out.db.repo"
})
public class JpaConfig {
}


