package com.example.payments.adapters.api.webhooks.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"eventId", "eventType", "eventDate", "payload"})
public record AuthorizeNetWebhookRequest(
        @NotBlank String eventId,
        @NotBlank String eventType,
        OffsetDateTime eventDate,
        Map<String, Object> payload) {
}
