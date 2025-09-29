package com.acme.payments.bootapi.payments;

import com.acme.payments.adapters.out.db.entity.OrderEntity;
import com.acme.payments.adapters.out.db.entity.PaymentIntentEntity;
import com.acme.payments.adapters.out.db.repo.ChargeRepository;
import com.acme.payments.adapters.out.db.repo.OrderRepository;
import com.acme.payments.adapters.out.db.repo.PaymentIntentRepository;
import com.acme.payments.adapters.out.gateway.AuthorizeNetClient;
import com.acme.payments.bootapi.error.ApiException;
import com.acme.payments.bootapi.security.MerchantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultPaymentsServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentIntentRepository intentRepository;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private MerchantContext merchantContext;
    @Mock
    private AuthorizeNetClient gateway;

    @InjectMocks
    private DefaultPaymentsService service;

    private PaymentDtos.Money money;

    @BeforeEach
    void setUp() {
        money = new PaymentDtos.Money("150.00", "INR");
    }

    @Test
    void validatePurchase_accepts_valid_amount() {
        service.validatePurchase(new PaymentDtos.PurchaseRequest("order", money, "token"));
    }

    @Test
    void validatePurchase_rejects_currency() {
        var moneyUsd = new PaymentDtos.Money("10.00", "USD");
        assertThatThrownBy(() -> service.validatePurchase(new PaymentDtos.PurchaseRequest("o", moneyUsd, "t")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only INR");
    }

    @Test
    void validatePurchase_rejects_bad_format() {
        var bad = new PaymentDtos.Money("1.0", "INR");
        assertThatThrownBy(() -> service.validatePurchase(new PaymentDtos.PurchaseRequest("o", bad, "t")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("two decimal");
    }

    @Test
    void createOrderIfAbsent_creates_new_order_and_records_payment() throws Exception {
        when(orderRepository.existsById("order-1")).thenReturn(false);
        OrderEntity savedOrder = new OrderEntity("order-1", "merchant-1", 15000, "INR", "NEW");
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(savedOrder));
        when(merchantContext.getMerchantId()).thenReturn("merchant-1");
        when(gateway.purchase("token", 15000L, "INR"))
                .thenReturn(gatewayResponse(true, "ref"));

        service.createOrderIfAbsent(new PaymentDtos.PurchaseRequest("order-1", money, "token"));

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());

        ArgumentCaptor<PaymentIntentEntity> intentCaptor = ArgumentCaptor.forClass(PaymentIntentEntity.class);
        verify(intentRepository).save(intentCaptor.capture());
        PaymentIntentEntity intent = intentCaptor.getValue();
        assertThat(extractField(intent, "status")).isEqualTo("CAPTURED");
        assertThat(extractField(intent, "gatewayRef")).isEqualTo("ref");
        verify(chargeRepository).save(any());

        OrderEntity persistedOrder = orderCaptor.getValue();
        assertThat(persistedOrder.getStatus()).isEqualTo("PAID");
    }

    @Test
    void createOrderIfAbsent_throws_when_gateway_declines() {
        when(orderRepository.existsById("order-2")).thenReturn(true);
        when(gateway.purchase("token", 15000L, "INR"))
                .thenReturn(gatewayResponse(false, null));

        assertThatThrownBy(() -> service.createOrderIfAbsent(new PaymentDtos.PurchaseRequest("order-2", money, "token")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("GATEWAY_DECLINED");
    }

    private AuthorizeNetClient.GatewayResponse gatewayResponse(boolean approved, String referenceId) {
        AuthorizeNetClient.GatewayResponse gr = new AuthorizeNetClient.GatewayResponse();
        gr.approved = approved;
        gr.referenceId = referenceId;
        return gr;
    }

    private Object extractField(Object target, String name) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}


