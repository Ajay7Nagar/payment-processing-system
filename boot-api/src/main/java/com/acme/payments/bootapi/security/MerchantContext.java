package com.acme.payments.bootapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class MerchantContext {
    public String getMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            Object m = jwt.getTokenAttributes().get("merchant_id");
            if (m != null) return m.toString();
        }
        return "test-merchant";
    }
}


