package com.acme.payments.bootapi.subscriptions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SubscriptionDtos {
    public enum ScheduleType { MONTHLY, WEEKLY, EVERY_N_DAYS }

    public record Schedule(
            @NotNull ScheduleType type,
            @Min(1) Integer nDays,
            @Min(0) Integer trialDays,
            @Min(1) Integer billingDay
    ) {}

    public record CreateRequest(
            @NotBlank String customerId,
            @NotNull Money amount,
            @NotNull Schedule schedule
    ) {}

    public record Money(
            @NotNull @Pattern(regexp = "^\\d+\\.\\d{2}$") String amount,
            @NotBlank String currency
    ) {}
}


