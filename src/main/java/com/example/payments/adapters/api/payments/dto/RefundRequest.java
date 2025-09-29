package com.example.payments.adapters.api.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequest(
        @NotNull UUID orderId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String lastFour,
        @NotNull UUID actorId) {
}
