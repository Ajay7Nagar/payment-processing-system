package com.example.payments.adapters.api.subscriptions;

import com.example.payments.adapters.api.subscriptions.dto.DunningHistoryResponse;
import com.example.payments.adapters.api.subscriptions.dto.SubscriptionActionRequest;
import com.example.payments.adapters.api.subscriptions.dto.SubscriptionCreateRequest;
import com.example.payments.adapters.api.subscriptions.dto.SubscriptionListResponse;
import com.example.payments.adapters.api.subscriptions.dto.SubscriptionResponse;
import com.example.payments.adapters.api.subscriptions.dto.SubscriptionScheduleResponse;
import com.example.payments.adapters.api.subscriptions.dto.SubscriptionUpdateRequest;
import com.example.payments.application.services.SubscriptionService;
import com.example.payments.domain.shared.CorrelationId;
import com.example.payments.domain.shared.IdempotencyKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Validated
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTIONS_CREATE')")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
            @Valid @RequestBody SubscriptionCreateRequest request) {
        IdempotencyKey key = new IdempotencyKey(idempotencyKey);
        CorrelationId correlationId = correlationIdHeader != null
                ? new CorrelationId(correlationIdHeader)
                : CorrelationId.newId();
        var subscription = subscriptionService.createSubscription(request.customerId(), request.planCode(),
                request.clientReference(), request.amount(), request.currency(), request.billingCycle(),
                request.trialEnd(), request.firstBillingAt(), key, correlationId, request.paymentMethodToken(),
                Optional.ofNullable(request.maxRetryAttempts()).orElse(4), request.intervalDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(subscription));
    }

    @GetMapping("/{subscriptionId}")
    @PreAuthorize("hasAuthority('SUBSCRIPTIONS_VIEW_ANY')")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable UUID subscriptionId) {
        var subscription = subscriptionService.getSubscription(subscriptionId);
        return ResponseEntity.ok(toResponse(subscription));
    }

    @PutMapping("/{subscriptionId}")
    @PreAuthorize("hasAuthority('SUBSCRIPTIONS_UPDATE')")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionUpdateRequest request) {
        var now = OffsetDateTime.now();
        var subscription = subscriptionService.updateSubscription(subscriptionId, request.planCode(),
                request.amount(), request.currency(), request.paymentMethodToken(), request.maxRetryAttempts(),
                request.intervalDays(), request.nextBillingAt(), now);
        return ResponseEntity.ok(toResponse(subscription));
    }

    @PostMapping("/{subscriptionId}/pause")
    @PreAuthorize("hasAuthority('SUBSCRIPTIONS_UPDATE')")
    public ResponseEntity<SubscriptionResponse> pauseSubscription(@PathVariable UUID subscriptionId) {
        subscriptionService.pauseSubscription(subscriptionId);
        var subscription = subscriptionService.getSubscription(subscriptionId);
        return ResponseEntity.ok(toResponse(subscription));
    }

    @PostMapping("/{subscriptionId}/resume")
    @PreAuthorize("hasAuthority('SUBSCRIPTIONS_UPDATE')")
    public ResponseEntity<SubscriptionResponse> resumeSubscription(@PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionActionRequest request) {
        OffsetDateTime nextBilling = request.nextBillingAt().orElse(OffsetDateTime.now().plusDays(1));
        var subscription = subscriptionService.resumeSubscription(subscriptionId, nextBilling);
        return ResponseEntity.ok(toResponse(subscription));
    }

    @PostMapping("/{subscriptionId}/cancel")
    @PreAuthorize("hasAuthority('SUBSCRIPTIONS_UPDATE')")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(@PathVariable UUID subscriptionId) {
        var subscription = subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(toResponse(subscription));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTIONS_VIEW_ANY')")
    public ResponseEntity<SubscriptionListResponse> listSubscriptions() {
        var subscriptions = subscriptionService.listSubscriptions().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(new SubscriptionListResponse(subscriptions));
    }

    @GetMapping("/{subscriptionId}/schedules")
    @PreAuthorize("hasAuthority('SUBSCRIPTIONS_VIEW_ANY')")
    public ResponseEntity<List<SubscriptionScheduleResponse>> listSchedules(@PathVariable UUID subscriptionId) {
        var schedules = subscriptionService.getSchedules(subscriptionId).stream()
                .map(SubscriptionScheduleResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/{subscriptionId}/dunning")
    @PreAuthorize("hasAuthority('SUBSCRIPTIONS_VIEW_ANY')")
    public ResponseEntity<List<DunningHistoryResponse>> listDunning(@PathVariable UUID subscriptionId) {
        var history = subscriptionService.getDunningHistory(subscriptionId).stream()
                .map(DunningHistoryResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(history);
    }

    private SubscriptionResponse toResponse(com.example.payments.domain.billing.Subscription subscription) {
        return new SubscriptionResponse(subscription.getId(), subscription.getCustomerId(), subscription.getPlanCode(),
                subscription.getAmount(), subscription.getCurrency(), subscription.getStatus(),
                subscription.getNextBillingAt(), subscription.getTrialEnd(), subscription.getDelinquentSince(),
                subscription.getRetryCount(), subscription.getMaxRetryAttempts(), subscription.getClientReference());
    }
}
