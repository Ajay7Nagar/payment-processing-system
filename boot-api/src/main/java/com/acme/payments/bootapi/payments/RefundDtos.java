package com.acme.payments.bootapi.payments;

import jakarta.validation.constraints.NotBlank;

public class RefundDtos {
    public record RefundRequest(
            @NotBlank String chargeId,
            PaymentDtos.Money amount
    ) {}
}
