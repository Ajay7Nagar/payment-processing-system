package com.example.payments.adapters.api.payments.dto;

import com.example.payments.domain.payments.PaymentOrderStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        PaymentOrderStatus status,
        String correlationId) {
}
