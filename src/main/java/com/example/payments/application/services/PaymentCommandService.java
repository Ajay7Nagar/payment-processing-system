package com.example.payments.application.services;

import com.example.payments.adapters.persistence.PaymentOrderRepository;
import com.example.payments.adapters.persistence.PaymentTransactionRepository;
import com.example.payments.adapters.persistence.RefundRepository;
import com.example.payments.domain.payments.GatewayTransactionResult;
import com.example.payments.domain.payments.Money;
import com.example.payments.domain.payments.PaymentException;
import com.example.payments.domain.payments.PaymentOrder;
import com.example.payments.domain.payments.PaymentOrderStatus;
import com.example.payments.domain.payments.PaymentTransaction;
import com.example.payments.domain.payments.PaymentTransactionType;
import com.example.payments.domain.payments.Refund;
import com.example.payments.domain.payments.RefundException;
import com.example.payments.domain.shared.CorrelationId;
import com.example.payments.domain.shared.IdempotencyKey;
import com.example.payments.infra.gateway.AuthorizeNetClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCommandService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandService.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundRepository refundRepository;
    private final AuthorizeNetClient authorizeNetClient;
    private final PaymentAuditService paymentAuditService;

    private final Counter purchaseCounter;
    private final Counter authorizeCounter;
    private final Counter captureCounter;
    private final Counter cancelCounter;
    private final Counter refundCounter;
    private final ObservationRegistry observationRegistry;

    public PaymentCommandService(PaymentOrderRepository paymentOrderRepository,
            PaymentTransactionRepository paymentTransactionRepository, RefundRepository refundRepository,
            AuthorizeNetClient authorizeNetClient, PaymentAuditService paymentAuditService, MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.refundRepository = refundRepository;
        this.authorizeNetClient = authorizeNetClient;
        this.paymentAuditService = paymentAuditService;
        this.purchaseCounter = Counter.builder("payments.purchase.count").register(meterRegistry);
        this.authorizeCounter = Counter.builder("payments.authorize.count").register(meterRegistry);
        this.captureCounter = Counter.builder("payments.capture.count").register(meterRegistry);
        this.cancelCounter = Counter.builder("payments.cancel.count").register(meterRegistry);
        this.refundCounter = Counter.builder("payments.refund.count").register(meterRegistry);
        this.observationRegistry = observationRegistry;
    }

    @Transactional
    public PaymentOrder purchase(UUID customerId, Money money, String paymentNonce,
            IdempotencyKey idempotencyKey, CorrelationId correlationId, String requestId) {
        ensureNoDuplicateRequest(requestId);

        PaymentOrder order = PaymentOrder.create(customerId, money, correlationId, requestId,
                idempotencyKey.value(), OffsetDateTime.now());
        paymentOrderRepository.save(order);

        GatewayTransactionResult result = Observation.createNotStarted("payments.purchase", observationRegistry)
                .lowCardinalityKeyValue("payments.request", "purchase")
                .observe(() -> authorizeNetClient.purchase(money, paymentNonce, order.getId().toString()));
        PaymentTransaction transaction = PaymentTransaction.record(order, PaymentTransactionType.PURCHASE,
                money, result.transactionId(), "SETTLED", result.processedAt(), result.responseCode(),
                result.responseMessage());
        order.addTransaction(transaction);
        order.markCaptured();

        paymentTransactionRepository.save(transaction);
        paymentOrderRepository.save(order);

        paymentAuditService.recordPurchase(order, transaction);
        purchaseCounter.increment();
        return order;
    }

    @Transactional
    public PaymentOrder authorize(UUID customerId, Money money, String paymentNonce,
            IdempotencyKey idempotencyKey, CorrelationId correlationId, String requestId) {
        ensureNoDuplicateRequest(requestId);

        PaymentOrder order = PaymentOrder.create(customerId, money, correlationId, requestId,
                idempotencyKey.value(), OffsetDateTime.now());
        paymentOrderRepository.save(order);

        GatewayTransactionResult result = Observation.createNotStarted("payments.authorize", observationRegistry)
                .lowCardinalityKeyValue("payments.request", "authorize")
                .observe(() -> authorizeNetClient.authorize(money, paymentNonce, order.getId().toString()));
        PaymentTransaction transaction = PaymentTransaction.record(order,
                PaymentTransactionType.AUTHORIZATION, money, result.transactionId(), "AUTHORIZED", result.processedAt(),
                result.responseCode(), result.responseMessage());
        order.addTransaction(transaction);
        order.markAuthorized();

        paymentTransactionRepository.save(transaction);
        paymentOrderRepository.save(order);

        paymentAuditService.recordAuthorization(order, transaction);
        authorizeCounter.increment();
        return order;
    }

    @Transactional
    public PaymentOrder capture(UUID orderId, Money amount, UUID actorId) {
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException("ORDER_NOT_FOUND", "Payment order not found"));

        if (order.getStatus() != PaymentOrderStatus.AUTHORIZED && order.getStatus() != PaymentOrderStatus.CREATED) {
            throw new PaymentException("INVALID_STATE", "Order cannot be captured from state " + order.getStatus());
        }

        PaymentTransaction authorization = order.getTransactions().stream()
                .filter(tx -> tx.getType() == PaymentTransactionType.AUTHORIZATION)
                .findFirst()
                .orElseThrow(() -> new PaymentException("AUTH_MISSING", "Authorization transaction missing"));

        GatewayTransactionResult result = Observation.createNotStarted("payments.capture", observationRegistry)
                .lowCardinalityKeyValue("payments.request", "capture")
                .observe(() -> authorizeNetClient.capture(amount, authorization.getAuthorizeNetTransactionId()));
        PaymentTransaction captureTx = PaymentTransaction.record(order, PaymentTransactionType.CAPTURE,
                amount, result.transactionId(), "CAPTURED", result.processedAt(), result.responseCode(),
                result.responseMessage());
        order.addTransaction(captureTx);
        order.markCaptured();

        paymentTransactionRepository.save(captureTx);
        paymentOrderRepository.save(order);

        paymentAuditService.recordCapture(order, captureTx, actorId);
        captureCounter.increment();
        return order;
    }

    @Transactional
    public PaymentOrder cancel(UUID orderId, UUID actorId) {
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException("ORDER_NOT_FOUND", "Payment order not found"));

        if (order.getStatus() != PaymentOrderStatus.AUTHORIZED && order.getStatus() != PaymentOrderStatus.CREATED) {
            throw new PaymentException("INVALID_STATE", "Order cannot be cancelled from state " + order.getStatus());
        }

        PaymentTransaction authorization = order.getTransactions().stream()
                .filter(tx -> tx.getType() == PaymentTransactionType.AUTHORIZATION)
                .findFirst()
                .orElseThrow(() -> new PaymentException("AUTH_MISSING", "Authorization transaction missing"));

        GatewayTransactionResult result = Observation.createNotStarted("payments.cancel", observationRegistry)
                .lowCardinalityKeyValue("payments.request", "cancel")
                .observe(() -> authorizeNetClient.voidTransaction(authorization.getAuthorizeNetTransactionId()));
        PaymentTransaction voidTx = PaymentTransaction.record(order, PaymentTransactionType.VOID,
                order.getMoney(), result.transactionId(), "VOIDED", result.processedAt(), result.responseCode(),
                result.responseMessage());
        order.addTransaction(voidTx);
        order.markCancelled();

        paymentTransactionRepository.save(voidTx);
        paymentOrderRepository.save(order);

        paymentAuditService.recordCancel(order, voidTx, actorId);
        cancelCounter.increment();
        return order;
    }

    @Transactional
    public Refund refund(UUID orderId, Money amount, String lastFour, UUID actorId) {
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException("ORDER_NOT_FOUND", "Payment order not found"));

        if (order.getStatus() != PaymentOrderStatus.CAPTURED && order.getStatus() != PaymentOrderStatus.SETTLED
                && order.getStatus() != PaymentOrderStatus.REFUNDED) {
            throw new PaymentException("INVALID_STATE", "Order cannot be refunded from state " + order.getStatus());
        }

        PaymentTransaction captureTx = order.getTransactions().stream()
                .filter(tx -> tx.getType() == PaymentTransactionType.CAPTURE
                        || tx.getType() == PaymentTransactionType.PURCHASE)
                .findFirst()
                .orElseThrow(() -> new PaymentException("CAPTURE_MISSING", "Capture transaction missing"));

        if (amount.amount().compareTo(order.getMoney().amount()) > 0) {
            throw new RefundException("INVALID_AMOUNT", "Refund amount exceeds original amount");
        }

        GatewayTransactionResult result = Observation.createNotStarted("payments.refund", observationRegistry)
                .lowCardinalityKeyValue("payments.request", "refund")
                .observe(() -> authorizeNetClient.refund(amount, captureTx.getAuthorizeNetTransactionId(), lastFour));
        Refund refund = Refund.record(captureTx, amount, "REFUNDED", result.transactionId(),
                result.processedAt());
        refundRepository.save(refund);

        PaymentTransaction refundTx = PaymentTransaction.record(order, PaymentTransactionType.REFUND, amount,
                result.transactionId(), "REFUNDED", result.processedAt(), result.responseCode(),
                result.responseMessage());
        order.addTransaction(refundTx);
        order.markRefunded();

        paymentTransactionRepository.save(refundTx);
        paymentOrderRepository.save(order);

        paymentAuditService.recordRefund(order, refundTx, actorId);
        refundCounter.increment();
        return refund;
    }

    private void ensureNoDuplicateRequest(String requestId) {
        Optional<PaymentOrder> existing = paymentOrderRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            throw new PaymentException("DUPLICATE_REQUEST", "Duplicate request detected");
        }
    }
}
