package com.acme.payments.bootapi.payments;

import com.acme.payments.adapters.out.db.entity.ChargeEntity;
import com.acme.payments.adapters.out.db.entity.RefundEntity;
import com.acme.payments.adapters.out.db.repo.ChargeRepository;
import com.acme.payments.adapters.out.db.repo.RefundRepository;
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
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultRefundsServiceTest {

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private MerchantContext merchantContext;

    @Mock
    private AuthorizeNetClient gateway;

    @InjectMocks
    private DefaultRefundsService service;


    @Test
    void validateRefundRequest_allows_null_amount() {
        service.validateRefundRequest(new RefundDtos.RefundRequest("charge-1", null));
    }

    @Test
    void validateRefundRequest_fails_on_currency_mismatch() {
        var request = new RefundDtos.RefundRequest("c", new PaymentDtos.Money("10.00", "USD"));
        assertThatThrownBy(() -> service.validateRefundRequest(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only INR is supported");
    }

    @Test
    void validateRefundRequest_fails_on_bad_format() {
        var request = new RefundDtos.RefundRequest("c", new PaymentDtos.Money("abc", "INR"));
        assertThatThrownBy(() -> service.validateRefundRequest(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid amount format");
    }

    @Test
    void validateRefundRequest_fails_on_wrong_scale() {
        var request = new RefundDtos.RefundRequest("c", new PaymentDtos.Money("10.0", "INR"));
        assertThatThrownBy(() -> service.validateRefundRequest(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("two decimal places");
    }

    @Test
    void validateRefundRequest_fails_on_out_of_range() {
        var request = new RefundDtos.RefundRequest("c", new PaymentDtos.Money("0.00", "INR"));
        assertThatThrownBy(() -> service.validateRefundRequest(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Amount must be between 0.01 and 1,000,000.00 INR");
    }

    @Test
    void createRefund_throws_when_charge_missing() {
        when(chargeRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createRefund(new RefundDtos.RefundRequest("missing", null)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "NOT_FOUND");
    }

    @Test
    void createRefund_throws_when_exceeds_original_amount() {
        ChargeEntity charge = chargeWithMinorAmount(10_00L);
        when(chargeRepository.findById("charge"))
                .thenReturn(Optional.of(charge));
        when(refundRepository.sumAmountMinorByChargeId("charge")).thenReturn(9_50L);

        var req = new RefundDtos.RefundRequest("charge", new PaymentDtos.Money("1.00", "INR"));

        assertThatThrownBy(() -> service.createRefund(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds original amount")
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createRefund_saves_completed_refund_when_gateway_approves() {
        ChargeEntity charge = chargeWithMinorAmount(5000L);
        when(chargeRepository.findById("ch-1")).thenReturn(Optional.of(charge));
        when(refundRepository.sumAmountMinorByChargeId("ch-1")).thenReturn(0L);
        when(merchantContext.getMerchantId()).thenReturn("merchant-42");
        when(gateway.refund("ch-1", 5000L)).thenReturn(gatewayResponse(true, "gw-1"));

        service.createRefund(new RefundDtos.RefundRequest("ch-1", null));

        ArgumentCaptor<RefundEntity> captor = ArgumentCaptor.forClass(RefundEntity.class);
        verify(refundRepository).save(captor.capture());
        RefundEntity saved = captor.getValue();
        assertThat(saved).isNotNull();
        assertThat(extractField(saved, "status")).isEqualTo("COMPLETED");
        assertThat(extractField(saved, "gatewayRef")).isEqualTo("gw-1");
        assertThat(extractField(saved, "amountMinor")).isEqualTo(5000L);
        assertThat(extractField(saved, "merchantId")).isEqualTo("merchant-42");
    }

    private ChargeEntity chargeWithMinorAmount(long amountMinor) {
        return new ChargeEntity() {
            @Override public long getAmountMinor() { return amountMinor; }
            @Override public String getId() { return "ch-1"; }
        };
    }

    private AuthorizeNetClient.GatewayResponse gatewayResponse(boolean approved, String ref) {
        AuthorizeNetClient.GatewayResponse gr = new AuthorizeNetClient.GatewayResponse();
        gr.approved = approved;
        gr.referenceId = ref;
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


