package com.example.payments.application.auth.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
}
