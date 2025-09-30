package com.example.payments.application.auth.event;

import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String email) {
}
