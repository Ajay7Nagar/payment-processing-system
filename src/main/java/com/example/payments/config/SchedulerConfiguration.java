package com.example.payments.config;

import com.example.payments.application.properties.SubscriptionProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SubscriptionProperties.class)
public class SchedulerConfiguration {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
