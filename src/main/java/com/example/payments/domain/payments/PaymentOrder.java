package com.example.payments.domain.payments;

import com.example.payments.domain.shared.CorrelationId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@Table(name = "payment_orders")
public class PaymentOrder {

    @Id
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentOrderStatus status;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("processedAt ASC")
    private List<PaymentTransaction> transactions = new ArrayList<>();

    protected PaymentOrder() {
    }

    private PaymentOrder(UUID id, UUID customerId, Money money, String correlationId, String requestId,
            String idempotencyKey, OffsetDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.amount = money.amount();
        this.currency = money.currency();
        this.status = PaymentOrderStatus.CREATED;
        this.correlationId = correlationId;
        this.requestId = requestId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static PaymentOrder create(UUID customerId, Money money, CorrelationId correlationId,
            String requestId, String idempotencyKey, OffsetDateTime createdAt) {
        Objects.requireNonNull(money, "money");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(createdAt, "createdAt");
        return new PaymentOrder(UUID.randomUUID(), customerId, money, correlationId.value(), requestId,
                idempotencyKey, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Money getMoney() {
        return new Money(amount, currency);
    }

    public PaymentOrderStatus getStatus() {
        return status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<PaymentTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void addTransaction(PaymentTransaction transaction) {
        transactions.add(transaction);
        transaction.setOrder(this);
        touch();
    }

    public void markAuthorized() {
        ensureStatus(PaymentOrderStatus.CREATED);
        this.status = PaymentOrderStatus.AUTHORIZED;
        touch();
    }

    public void markCaptured() {
        ensureStatus(PaymentOrderStatus.AUTHORIZED, PaymentOrderStatus.CREATED);
        this.status = PaymentOrderStatus.CAPTURED;
        touch();
    }

    public void markSettled() {
        ensureStatus(PaymentOrderStatus.CAPTURED);
        this.status = PaymentOrderStatus.SETTLED;
        touch();
    }

    public void markCancelled() {
        ensureStatus(PaymentOrderStatus.CREATED, PaymentOrderStatus.AUTHORIZED);
        this.status = PaymentOrderStatus.CANCELLED;
        touch();
    }

    public void markRefunded() {
        ensureStatus(PaymentOrderStatus.SETTLED, PaymentOrderStatus.CAPTURED);
        this.status = PaymentOrderStatus.REFUNDED;
        touch();
    }

    public void markFailed() {
        this.status = PaymentOrderStatus.FAILED;
        touch();
    }

    private void ensureStatus(PaymentOrderStatus... allowed) {
        for (PaymentOrderStatus status : allowed) {
            if (this.status == status) {
                return;
            }
        }
        throw new IllegalStateException("Invalid state transition from " + this.status);
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    public BigDecimal totalCapturedAmount() {
        return transactions.stream()
                .filter(tx -> tx.getType() == PaymentTransactionType.CAPTURE || tx.getType() == PaymentTransactionType.PURCHASE)
                .map(PaymentTransaction::getMoney)
                .map(Money::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalRefundedAmount() {
        return transactions.stream()
                .filter(tx -> tx.getType() == PaymentTransactionType.REFUND)
                .map(PaymentTransaction::getMoney)
                .map(Money::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
