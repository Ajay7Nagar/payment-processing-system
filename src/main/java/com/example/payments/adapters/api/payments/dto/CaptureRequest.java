package com.example.payments.adapters.api.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CaptureRequest(
        @NotNull UUID orderId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull UUID actorId) {
}
