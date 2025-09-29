package com.example.payments.infra.observability;

import com.example.payments.domain.shared.CorrelationId;

final class CorrelationIdContext {

    private static final ThreadLocal<CorrelationId> CURRENT = new ThreadLocal<>();

    private CorrelationIdContext() {
    }

    static void set(CorrelationId correlationId) {
        CURRENT.set(correlationId);
    }

    static CorrelationId get() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }
}

