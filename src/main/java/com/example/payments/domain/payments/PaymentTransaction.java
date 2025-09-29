package com.example.payments.domain.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PaymentOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentTransactionType type;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "authorize_net_transaction_id")
    private String authorizeNetTransactionId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    @Column(name = "response_code")
    private String responseCode;

    @Column(name = "response_message")
    private String responseMessage;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(UUID id, PaymentOrder order, PaymentTransactionType type, Money money,
            String authorizeNetTransactionId, String status, OffsetDateTime processedAt, String responseCode,
            String responseMessage) {
        this.id = id;
        this.order = order;
        this.type = type;
        this.amount = money.amount();
        this.authorizeNetTransactionId = authorizeNetTransactionId;
        this.status = status;
        this.processedAt = processedAt;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public static PaymentTransaction record(PaymentOrder order, PaymentTransactionType type, Money money,
            String authorizeNetTransactionId, String status, OffsetDateTime processedAt, String responseCode,
            String responseMessage) {
        return new PaymentTransaction(UUID.randomUUID(), order, type, money, authorizeNetTransactionId,
                status, processedAt, responseCode, responseMessage);
    }

    public UUID getId() {
        return id;
    }

    public PaymentOrder getOrder() {
        return order;
    }

    void setOrder(PaymentOrder order) {
        this.order = order;
    }

    public PaymentTransactionType getType() {
        return type;
    }

    public Money getMoney() {
        return new Money(amount, order.getMoney().currency());
    }

    public String getAuthorizeNetTransactionId() {
        return authorizeNetTransactionId;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }
}
