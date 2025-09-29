package com.acme.payments.bootapi.payments;

import com.acme.payments.adapters.out.db.entity.ChargeEntity;
import com.acme.payments.adapters.out.db.entity.OrderEntity;
import com.acme.payments.adapters.out.db.entity.PaymentIntentEntity;
import com.acme.payments.adapters.out.db.repo.ChargeRepository;
import com.acme.payments.adapters.out.db.repo.OrderRepository;
import com.acme.payments.adapters.out.db.repo.PaymentIntentRepository;
import com.acme.payments.adapters.out.gateway.AuthorizeNetClient;
import com.acme.payments.bootapi.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPaymentsOpsServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentIntentRepository intentRepository;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private AuthorizeNetClient gateway;

    @InjectMocks
    private DefaultPaymentsOpsService service;

    private PaymentDtos.Money money;

    @BeforeEach
    void setUp() {
        money = new PaymentDtos.Money("10.00", "INR");
    }

    @Test
    void authorize_creates_intent_when_gateway_approves() {
        OrderEntity order = mock(OrderEntity.class);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(gateway.authorize("tok", 1000L, "INR"))
                .thenReturn(gatewayResponse(true, "ref"));

        service.authorize(new PaymentDtos.AuthorizeRequest("order-1", money, "tok"));

        ArgumentCaptor<PaymentIntentEntity> captor = ArgumentCaptor.forClass(PaymentIntentEntity.class);
        verify(intentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("AUTHORIZED");
        assertThat(captor.getValue().getGatewayRef()).isEqualTo("ref");
    }

    @Test
    void authorize_throws_when_order_missing() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authorize(new PaymentDtos.AuthorizeRequest("missing", money, "tok")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void capture_saves_charge_and_updates_intent_status() {
        PaymentIntentEntity intent = new PaymentIntentEntity();
        when(intentRepository.findById("auth-1")).thenReturn(Optional.of(intent));
        when(gateway.capture("auth-1", 1200L))
                .thenReturn(gatewayResponse(true, "cap-ref"));

        service.capture(new PaymentDtos.CaptureRequest("auth-1", new PaymentDtos.Money("12.00", "INR")));

        ArgumentCaptor<ChargeEntity> chargeCaptor = ArgumentCaptor.forClass(ChargeEntity.class);
        verify(chargeRepository).save(chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getAmountMinor()).isEqualTo(1200L);
        verify(intentRepository).save(intent);
        assertThat(intent.getStatus()).isEqualTo("CAPTURED");
    }

    @Test
    void capture_throws_when_intent_missing() {
        when(intentRepository.findById("auth")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.capture(new PaymentDtos.CaptureRequest("auth", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Authorization not found");
    }

    @Test
    void cancel_returns_cached_when_already_canceled() throws Exception {
        PaymentIntentEntity intent = new PaymentIntentEntity();
        setIntentStatus(intent, "CANCELED");
        when(intentRepository.findById("auth")).thenReturn(Optional.of(intent));

        PaymentDtos.CancelResponse response = service.cancel(new PaymentDtos.CancelRequest("auth"));
        assertThat(response.status()).isEqualTo("CANCELED");
    }

    @Test
    void cancel_throws_when_captured() throws Exception {
        PaymentIntentEntity intent = new PaymentIntentEntity();
        setIntentStatus(intent, "CAPTURED");
        when(intentRepository.findById("auth")).thenReturn(Optional.of(intent));

        assertThatThrownBy(() -> service.cancel(new PaymentDtos.CancelRequest("auth")))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cancel_voids_through_gateway_and_updates_status() throws Exception {
        PaymentIntentEntity intent = new PaymentIntentEntity();
        setIntentStatus(intent, "AUTHORIZED");
        setIntentGatewayRef(intent, "gw");
        when(intentRepository.findById("auth")).thenReturn(Optional.of(intent));
        when(gateway.voidAuth("gw")).thenReturn(gatewayResponse(true, "void-ref"));

        PaymentDtos.CancelResponse response = service.cancel(new PaymentDtos.CancelRequest("auth"));

        assertThat(response.status()).isEqualTo("CANCELED");
        verify(intentRepository).save(intent);
        assertThat(extractField(intent, "status")).isEqualTo("CANCELED");
    }

    private void setIntentStatus(PaymentIntentEntity intent, String status) {
        setField(intent, "status", status);
        setField(intent, "id", "auth");
    }

    private void setIntentGatewayRef(PaymentIntentEntity intent, String ref) {
        setField(intent, "gatewayRef", ref);
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

    private void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}


