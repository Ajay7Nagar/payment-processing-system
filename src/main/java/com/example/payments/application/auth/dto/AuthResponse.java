package com.example.payments.application.auth.dto;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record AuthResponse(UUID userId,
        String email,
        String fullName,
        UUID customerId,
        Optional<String> accessToken,
        Optional<String> refreshToken,
        Optional<Instant> refreshTokenExpiresAt) {

    public static AuthResponse registration(UUID userId, String email, String fullName, UUID customerId) {
        return new AuthResponse(userId, email, fullName, customerId, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static AuthResponse authenticated(UUID userId, String email, String fullName, UUID customerId,
            String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
        return new AuthResponse(userId, email, fullName, customerId,
                Optional.ofNullable(accessToken), Optional.ofNullable(refreshToken),
                Optional.ofNullable(refreshTokenExpiresAt));
    }
}
