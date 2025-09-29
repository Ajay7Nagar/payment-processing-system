package com.acme.payments.bootapi.payments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class PaymentDtos {
    public record Money(
            @NotNull @Pattern(regexp = "^\\d+\\.\\d{2}$") String amount,
            @NotBlank String currency
    ) {}

    public record PurchaseRequest(
            @NotBlank String orderId,
            @NotNull Money amount,
            @NotBlank String paymentToken
    ) {}

    public record AuthorizeRequest(
            @NotBlank String orderId,
            @NotNull Money amount,
            @NotBlank String paymentToken
    ) {}

    public record CaptureRequest(
            @NotBlank String authorizationId,
            Money amount
    ) {}

    public record CancelRequest(
            @NotBlank String authorizationId
    ) {}

    public record Payment(
            String id,
            Money amount,
            String status,
            String createdAt
    ) {}

    public record CancelResponse(
            String authorizationId,
            String status
    ) {}
}
