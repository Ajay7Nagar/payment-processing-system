package com.example.payments.adapters.api.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseRequest(
        @NotNull UUID customerId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotBlank String paymentNonce,
        @NotBlank String idempotencyKey,
        @NotBlank String correlationId,
        @NotBlank String requestId) {
}
