package com.example.payments.infra.gateway.sdk;

import net.authorize.Environment;
import com.example.payments.infra.gateway.AuthorizeNetProperties;
import org.springframework.stereotype.Component;

@Component
public class AuthorizeNetEnvironmentResolver {

    public Environment resolve(AuthorizeNetProperties properties) {
        String endpoint = properties.getEndpoint();
        if (endpoint == null || endpoint.contains("apitest.authorize.net")) {
            return Environment.SANDBOX;
        }
        if (endpoint.contains("api2.authorize.net") || endpoint.contains("api.authorize.net")) {
            return Environment.PRODUCTION;
        }
        return Environment.CUSTOM;
    }

    public String resolveCustomBaseUrl(AuthorizeNetProperties properties) {
        return properties.getEndpoint();
    }
}
