package com.example.payments.domain.billing;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "plan_code", nullable = false)
    private String planCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private SubscriptionBillingCycle billingCycle;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "payment_method_token", nullable = false)
    private String paymentMethodToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Column(name = "client_reference", nullable = false)
    private String clientReference;

    @Column(name = "trial_end")
    private OffsetDateTime trialEnd;

    @Column(name = "next_billing_at", nullable = false)
    private OffsetDateTime nextBillingAt;

    @Column(name = "delinquent_since")
    private OffsetDateTime delinquentSince;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retry_attempts", nullable = false)
    private int maxRetryAttempts;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SubscriptionSchedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DunningHistory> dunningHistory = new ArrayList<>();

    protected Subscription() {
    }

    private Subscription(UUID id, UUID customerId, String planCode,
            SubscriptionBillingCycle billingCycle, Integer intervalDays, BigDecimal amount, String currency,
            String paymentMethodToken, SubscriptionStatus status, String clientReference, OffsetDateTime trialEnd,
            OffsetDateTime nextBillingAt, OffsetDateTime delinquentSince, int retryCount, int maxRetryAttempts,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.planCode = planCode;
        this.billingCycle = billingCycle;
        this.intervalDays = intervalDays;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethodToken = paymentMethodToken;
        this.status = status;
        this.clientReference = clientReference;
        this.trialEnd = trialEnd;
        this.nextBillingAt = nextBillingAt;
        this.delinquentSince = delinquentSince;
        this.retryCount = retryCount;
        this.maxRetryAttempts = maxRetryAttempts;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Subscription create(UUID customerId, String planCode, SubscriptionBillingCycle cycle,
            Integer intervalDays, BigDecimal amount, String currency, String paymentMethodToken, String clientReference,
            OffsetDateTime trialEnd, OffsetDateTime firstBilling, int maxRetryAttempts, OffsetDateTime now) {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(planCode, "planCode");
        Objects.requireNonNull(cycle, "cycle");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(paymentMethodToken, "paymentMethodToken");
        Objects.requireNonNull(clientReference, "clientReference");
        Objects.requireNonNull(firstBilling, "firstBilling");
        Objects.requireNonNull(now, "now");
        return new Subscription(UUID.randomUUID(), customerId, planCode, cycle, intervalDays, amount,
                currency, paymentMethodToken, SubscriptionStatus.ACTIVE, clientReference, trialEnd, firstBilling, null,
                0, maxRetryAttempts, now, now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public SubscriptionBillingCycle getBillingCycle() {
        return billingCycle;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentMethodToken() {
        return paymentMethodToken;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public String getClientReference() {
        return clientReference;
    }

    public OffsetDateTime getTrialEnd() {
        return trialEnd;
    }

    public OffsetDateTime getNextBillingAt() {
        return nextBillingAt;
    }

    public OffsetDateTime getDelinquentSince() {
        return delinquentSince;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts, OffsetDateTime now) {
        if (maxRetryAttempts <= 0) {
            throw new IllegalArgumentException("maxRetryAttempts must be positive");
        }
        this.maxRetryAttempts = maxRetryAttempts;
        touch(now);
    }

    public void setIntervalDays(Integer intervalDays, OffsetDateTime now) {
        this.intervalDays = intervalDays;
        touch(now);
    }

    public void setNextBillingAt(OffsetDateTime nextBillingAt) {
        this.nextBillingAt = nextBillingAt;
    }

    public void updatePlan(String newPlanCode, BigDecimal newAmount, String newCurrency, OffsetDateTime now) {
        if (newPlanCode != null && !newPlanCode.isBlank()) {
            this.planCode = newPlanCode;
        }
        if (newAmount != null) {
            this.amount = newAmount;
        }
        if (newCurrency != null && !newCurrency.isBlank()) {
            this.currency = newCurrency;
        }
        touch(now);
    }

    public void updatePaymentMethod(String newPaymentMethodToken, OffsetDateTime now) {
        if (newPaymentMethodToken == null || newPaymentMethodToken.isBlank()) {
            throw new IllegalArgumentException("payment method token cannot be blank");
        }
        this.paymentMethodToken = newPaymentMethodToken;
        touch(now);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<SubscriptionSchedule> getSchedules() {
        return Collections.unmodifiableList(schedules);
    }

    public List<DunningHistory> getDunningHistory() {
        return Collections.unmodifiableList(dunningHistory);
    }

    public void pause(OffsetDateTime now) {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Only active subscriptions can be paused");
        }
        status = SubscriptionStatus.PAUSED;
        touch(now);
    }

    public void resume(OffsetDateTime nextBilling, OffsetDateTime now) {
        if (status != SubscriptionStatus.PAUSED) {
            throw new IllegalStateException("Only paused subscriptions can be resumed");
        }
        status = SubscriptionStatus.ACTIVE;
        nextBillingAt = nextBilling;
        touch(now);
    }

    public void cancel(OffsetDateTime now) {
        status = SubscriptionStatus.CANCELLED;
        touch(now);
    }

    public void markDelinquent(OffsetDateTime now) {
        status = SubscriptionStatus.DELINQUENT;
        if (delinquentSince == null) {
            delinquentSince = now;
        }
        touch(now);
    }

    public void markCompleted(OffsetDateTime now) {
        status = SubscriptionStatus.COMPLETED;
        touch(now);
    }

    public void recordSuccessfulCharge(OffsetDateTime now) {
        status = SubscriptionStatus.ACTIVE;
        retryCount = 0;
        delinquentSince = null;
        nextBillingAt = calculateNextBillingAfter(nextBillingAt);
        touch(now);
    }

    public void recordFailedCharge(OffsetDateTime nextAttempt, OffsetDateTime now) {
        retryCount += 1;
        if (delinquentSince == null) {
            delinquentSince = now;
        }
        status = SubscriptionStatus.DELINQUENT;
        nextBillingAt = nextAttempt;
        touch(now);
    }

    public boolean hasExceededRetryAttempts() {
        return retryCount >= maxRetryAttempts;
    }

    public boolean shouldAutoCancel(OffsetDateTime now, int autoCancelDays) {
        return delinquentSince != null && delinquentSince.plusDays(autoCancelDays).isBefore(now);
    }

    public OffsetDateTime calculateNextBillingAfter(OffsetDateTime reference) {
        return switch (billingCycle) {
            case DAILY -> reference.plusDays(1);
            case WEEKLY -> reference.plusWeeks(1);
            case MONTHLY -> reference.plusMonths(1);
            case YEARLY -> reference.plusYears(1);
            case CUSTOM -> reference.plusDays(intervalDays != null ? intervalDays : 30);
        };
    }

    public void addSchedule(SubscriptionSchedule schedule) {
        schedules.add(schedule);
        schedule.setSubscription(this);
    }

    public void addDunningEvent(DunningHistory history) {
        dunningHistory.add(history);
        history.setSubscription(this);
    }

    private void touch(OffsetDateTime now) {
        updatedAt = now;
    }
}
