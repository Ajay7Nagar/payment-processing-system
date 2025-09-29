package com.acme.payments.bootapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MerchantContext {
    public String getMerchantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof java.util.Map) {
            Object m = ((java.util.Map<?,?>) auth.getDetails()).get("merchant_id");
            if (m != null) return m.toString();
        }
        return "test-merchant";
    }
}


