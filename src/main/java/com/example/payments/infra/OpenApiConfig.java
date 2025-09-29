package com.example.payments.infra;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI platformOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Payment Processing Platform API")
                .version("0.1.0")
                .description("Placeholder OpenAPI definition for the modular monolith skeleton."));
    }
}

