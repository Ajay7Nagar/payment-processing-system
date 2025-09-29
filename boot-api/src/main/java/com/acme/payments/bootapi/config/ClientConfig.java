package com.acme.payments.bootapi.config;

import com.acme.payments.adapters.out.gateway.AuthorizeNetClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    @Bean
    public AuthorizeNetClient authorizeNetClient(
            @Value("${authorizenet.apiLoginId:}") String apiLoginId,
            @Value("${authorizenet.transactionKey:}") String transactionKey,
            @Value("${authorizenet.sandbox:true}") boolean sandbox,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper
    ) {
        return new AuthorizeNetClient(apiLoginId, transactionKey, sandbox, meterRegistry, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}


