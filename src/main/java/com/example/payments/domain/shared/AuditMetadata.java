package com.example.payments.domain.shared;

import java.util.Map;

public record AuditMetadata(Map<String, Object> values) {
    public AuditMetadata {
        values = Map.copyOf(values);
    }
}
