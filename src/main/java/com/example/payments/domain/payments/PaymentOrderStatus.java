package com.example.payments.domain.payments;

public enum PaymentOrderStatus {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    CANCELLED,
    REFUNDED,
    FAILED
}
