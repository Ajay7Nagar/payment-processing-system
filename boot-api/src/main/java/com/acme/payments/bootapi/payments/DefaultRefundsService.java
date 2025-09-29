package com.acme.payments.bootapi.payments;

import com.acme.payments.adapters.out.db.entity.ChargeEntity;
import com.acme.payments.adapters.out.db.entity.RefundEntity;
import com.acme.payments.adapters.out.db.repo.ChargeRepository;
import com.acme.payments.adapters.out.db.repo.RefundRepository;
import com.acme.payments.bootapi.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DefaultRefundsService implements RefundsService {
    private static final BigDecimal MIN = new BigDecimal("0.01");
    private static final BigDecimal MAX = new BigDecimal("1000000.00");

    private final ChargeRepository chargeRepository;
    private final RefundRepository refundRepository;
    private final com.acme.payments.adapters.out.gateway.AuthorizeNetClient gateway;

    private final com.acme.payments.bootapi.security.MerchantContext merchantContext;

    public DefaultRefundsService(ChargeRepository chargeRepository, RefundRepository refundRepository,
                                 com.acme.payments.bootapi.security.MerchantContext merchantContext,
                                 com.acme.payments.adapters.out.gateway.AuthorizeNetClient gateway) {
        this.chargeRepository = chargeRepository;
        this.refundRepository = refundRepository;
        this.merchantContext = merchantContext;
        this.gateway = gateway;
    }

    @Override
    public void validateRefundRequest(RefundDtos.RefundRequest req) {
        if (req.amount() == null) {
            return; // full refund, amount optional
        }
        PaymentDtos.Money m = req.amount();
        if (!"INR".equals(m.currency())) {
            throw ApiException.unprocessable("CURRENCY_NOT_SUPPORTED", "Only INR is supported");
        }
        BigDecimal amt;
        try {
            amt = new BigDecimal(m.amount());
        } catch (NumberFormatException ex) {
            throw ApiException.unprocessable("AMOUNT_OUT_OF_RANGE", "Invalid amount format");
        }
        if (amt.scale() != 2) {
            throw ApiException.unprocessable("AMOUNT_OUT_OF_RANGE", "Amount must have two decimal places");
        }
        if (amt.compareTo(MIN) < 0 || amt.compareTo(MAX) > 0) {
            throw ApiException.unprocessable("AMOUNT_OUT_OF_RANGE", "Amount must be between 0.01 and 1,000,000.00 INR");
        }
    }

    @Override
    public void createRefund(RefundDtos.RefundRequest req) {
        ChargeEntity charge = chargeRepository.findById(req.chargeId())
                .orElseThrow(() -> ApiException.unprocessable("NOT_FOUND", "Charge not found"));
        long requestedMinor = req.amount() == null ? charge.getAmountMinor() : new BigDecimal(req.amount().amount()).movePointRight(2).longValueExact();
        long alreadyRefunded = refundRepository.sumAmountMinorByChargeId(req.chargeId());
        if (alreadyRefunded + requestedMinor > charge.getAmountMinor()) {
            throw new ApiException("CONFLICT", "Refund exceeds original amount", HttpStatus.CONFLICT);
        }
        RefundEntity r = new RefundEntity();
        try {
            java.lang.reflect.Field f;
            f = RefundEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(r, "rf-" + req.chargeId() + "-" + (alreadyRefunded + 1));
            f = RefundEntity.class.getDeclaredField("charge"); f.setAccessible(true); f.set(r, charge);
            f = RefundEntity.class.getDeclaredField("amountMinor"); f.setAccessible(true); f.set(r, requestedMinor);
            f = RefundEntity.class.getDeclaredField("status"); f.setAccessible(true);
            com.acme.payments.adapters.out.gateway.AuthorizeNetClient.GatewayResponse gr = gateway.refund(charge.getId(), requestedMinor);
            f.set(r, gr.approved ? "COMPLETED" : "REQUESTED");
            f = RefundEntity.class.getDeclaredField("merchantId"); f.setAccessible(true); f.set(r, merchantContext.getMerchantId());
            if (gr.referenceId != null) { f = RefundEntity.class.getDeclaredField("gatewayRef"); f.setAccessible(true); f.set(r, gr.referenceId); }
        } catch (Exception e) { throw new RuntimeException(e); }
        refundRepository.save(r);
    }
}
