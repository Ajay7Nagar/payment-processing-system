package com.example.payments.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.example.payments.adapters.persistence.PaymentOrderRepository;
import com.example.payments.adapters.persistence.PaymentTransactionRepository;
import com.example.payments.adapters.persistence.RefundRepository;
import com.example.payments.domain.payments.GatewayTransactionResult;
import com.example.payments.domain.payments.Money;
import com.example.payments.domain.payments.PaymentException;
import com.example.payments.domain.payments.PaymentOrder;
import com.example.payments.domain.payments.PaymentTransaction;
import com.example.payments.domain.payments.PaymentTransactionType;
import com.example.payments.domain.payments.Refund;
import com.example.payments.domain.shared.CorrelationId;
import com.example.payments.domain.shared.IdempotencyKey;
import com.example.payments.infra.gateway.AuthorizeNetClient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private AuthorizeNetClient authorizeNetClient;
    @Mock
    private PaymentAuditService paymentAuditService;

    private PaymentCommandService paymentCommandService;

    private UUID customerId;
    private Money money;
    private IdempotencyKey idempotencyKey;
    private CorrelationId correlationId;

    @Captor
    private ArgumentCaptor<PaymentTransaction> transactionCaptor;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        money = new Money(new BigDecimal("100.00"), "USD");
        idempotencyKey = new IdempotencyKey("key-12345678");
        correlationId = CorrelationId.newId();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        paymentCommandService = new PaymentCommandService(paymentOrderRepository, paymentTransactionRepository,
                refundRepository, authorizeNetClient, paymentAuditService, meterRegistry, ObservationRegistry.create());
    }

    @Test
    void purchase_shouldCreateOrderAndTransaction() {
        doReturn(GatewayTransactionResult.success("tx123", "1", "Approved", OffsetDateTime.now()))
                .when(authorizeNetClient).purchase(any(), any(), any());
        doReturn(Optional.empty()).when(paymentOrderRepository).findByRequestId(any());

        PaymentOrder order = paymentCommandService.purchase(customerId, money, "nonce", idempotencyKey,
                correlationId, "req123");

        assertThat(order.getStatus()).isEqualTo(com.example.payments.domain.payments.PaymentOrderStatus.CAPTURED);
        verify(paymentOrderRepository, times(2)).save(order);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getType()).isEqualTo(PaymentTransactionType.PURCHASE);
    }

    @Test
    void authorize_shouldCreateAuthorization() {
        doReturn(GatewayTransactionResult.success("tx123", "1", "Approved", OffsetDateTime.now()))
                .when(authorizeNetClient).authorize(any(), any(), any());
        doReturn(Optional.empty()).when(paymentOrderRepository).findByRequestId(any());

        PaymentOrder order = paymentCommandService.authorize(customerId, money, "nonce", idempotencyKey,
                correlationId, "req123");

        assertThat(order.getStatus()).isEqualTo(com.example.payments.domain.payments.PaymentOrderStatus.AUTHORIZED);
        verify(paymentOrderRepository, times(2)).save(order);
    }

    @Test
    void capture_shouldThrowWhenOrderMissing() {
        doReturn(Optional.empty()).when(paymentOrderRepository).findById(any());

        assertThatThrownBy(() -> paymentCommandService.capture(UUID.randomUUID(), money,
                UUID.randomUUID())).isInstanceOf(PaymentException.class)
                .hasMessageContaining("Payment order not found");
    }

    @Test
    void refund_shouldPersistRefund() {
        PaymentOrder order = PaymentOrder.create(customerId, money, correlationId, "req123",
                idempotencyKey.value(), OffsetDateTime.now());
        PaymentTransaction transaction = PaymentTransaction.record(order, PaymentTransactionType.PURCHASE,
                money, "tx123", "SETTLED", OffsetDateTime.now(), "1", "Approved");
        order.addTransaction(transaction);
        order.markCaptured();

        doReturn(Optional.of(order)).when(paymentOrderRepository).findById(any());
        doReturn(GatewayTransactionResult.success("tx124", "1", "Refunded", OffsetDateTime.now()))
                .when(authorizeNetClient).refund(any(), any(), any());

        Refund refund = paymentCommandService.refund(order.getId(), new Money(new BigDecimal("10.00"),
                "USD"), "1234", UUID.randomUUID());

        assertThat(refund.getStatus()).isEqualTo("REFUNDED");
        verify(refundRepository).save(refund);
    }
}
