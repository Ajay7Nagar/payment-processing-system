package com.example.payments.domain.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private PaymentTransaction transaction;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "authorize_net_transaction_id")
    private String authorizeNetTransactionId;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    protected Refund() {
    }

    public Refund(UUID id, PaymentTransaction transaction, Money money, String status,
            String authorizeNetTransactionId, OffsetDateTime processedAt) {
        this.id = id;
        this.transaction = transaction;
        this.amount = money.amount();
        this.status = status;
        this.authorizeNetTransactionId = authorizeNetTransactionId;
        this.processedAt = processedAt;
    }

    public static Refund record(PaymentTransaction transaction, Money money, String status,
            String authorizeNetTransactionId, OffsetDateTime processedAt) {
        return new Refund(UUID.randomUUID(), transaction, money, status, authorizeNetTransactionId,
                processedAt);
    }

    public UUID getId() {
        return id;
    }

    public PaymentTransaction getTransaction() {
        return transaction;
    }

    public Money getMoney() {
        return new Money(amount, transaction.getMoney().currency());
    }

    public String getStatus() {
        return status;
    }

    public String getAuthorizeNetTransactionId() {
        return authorizeNetTransactionId;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
