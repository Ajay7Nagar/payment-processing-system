package com.example.payments.adapters.api.payments.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CancelRequest(
        @NotNull UUID orderId,
        @NotNull UUID actorId) {
}
