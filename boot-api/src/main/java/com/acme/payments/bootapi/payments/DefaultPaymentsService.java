package com.acme.payments.bootapi.payments;

import com.acme.payments.bootapi.error.ApiException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DefaultPaymentsService implements PaymentsService {
    private static final BigDecimal MIN = new BigDecimal("0.50");
    private static final BigDecimal MAX = new BigDecimal("1000000.00");

    private final com.acme.payments.adapters.out.db.repo.OrderRepository orderRepository;
    private final com.acme.payments.adapters.out.db.repo.PaymentIntentRepository intentRepository;
    private final com.acme.payments.adapters.out.db.repo.ChargeRepository chargeRepository;
    private final com.acme.payments.bootapi.security.MerchantContext merchantContext;
    private final com.acme.payments.adapters.out.gateway.AuthorizeNetClient gateway;

    public DefaultPaymentsService(com.acme.payments.adapters.out.db.repo.OrderRepository orderRepository,
                                  com.acme.payments.adapters.out.db.repo.PaymentIntentRepository intentRepository,
                                  com.acme.payments.adapters.out.db.repo.ChargeRepository chargeRepository,
                                  com.acme.payments.bootapi.security.MerchantContext merchantContext,
                                  com.acme.payments.adapters.out.gateway.AuthorizeNetClient gateway) {
        this.orderRepository = orderRepository;
        this.intentRepository = intentRepository;
        this.chargeRepository = chargeRepository;
        this.merchantContext = merchantContext;
        this.gateway = gateway;
    }

    @Override
    public void validatePurchase(PaymentDtos.PurchaseRequest req) {
        validateMoney(req.amount());
        // BIN/merchant caps will be added later (Track H)
    }

    @Override
    public void createOrderIfAbsent(PaymentDtos.PurchaseRequest req) {
        if (!orderRepository.existsById(req.orderId())) {
            String merchantId = merchantContext.getMerchantId();
            com.acme.payments.adapters.out.db.entity.OrderEntity e = new com.acme.payments.adapters.out.db.entity.OrderEntity(
                    req.orderId(), merchantId, new java.math.BigDecimal(req.amount().amount()).movePointRight(2).longValueExact(),
                    req.amount().currency(), "NEW");
            orderRepository.save(e);
        }
        // Call gateway purchase for happy-path
        long amountMinor = new java.math.BigDecimal(req.amount().amount()).movePointRight(2).longValueExact();
        com.acme.payments.adapters.out.gateway.AuthorizeNetClient.GatewayResponse gr = gateway.purchase(req.paymentToken(), amountMinor, req.amount().currency());
        if (!gr.approved) {
            throw ApiException.gateway("GATEWAY_DECLINED", "Purchase declined");
        }
        // Persist intent and charge with gateway ref
        com.acme.payments.adapters.out.db.entity.OrderEntity order = orderRepository.findById(req.orderId()).orElseThrow();
        com.acme.payments.adapters.out.db.entity.PaymentIntentEntity intent = new com.acme.payments.adapters.out.db.entity.PaymentIntentEntity();
        try {
            java.lang.reflect.Field f;
            f = com.acme.payments.adapters.out.db.entity.PaymentIntentEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(intent, "pi-" + req.orderId());
            f = com.acme.payments.adapters.out.db.entity.PaymentIntentEntity.class.getDeclaredField("order"); f.setAccessible(true); f.set(intent, order);
            f = com.acme.payments.adapters.out.db.entity.PaymentIntentEntity.class.getDeclaredField("type"); f.setAccessible(true); f.set(intent, "PURCHASE");
            f = com.acme.payments.adapters.out.db.entity.PaymentIntentEntity.class.getDeclaredField("status"); f.setAccessible(true); f.set(intent, "CAPTURED");
            f = com.acme.payments.adapters.out.db.entity.PaymentIntentEntity.class.getDeclaredField("gatewayRef"); f.setAccessible(true); f.set(intent, gr.referenceId);
        } catch (Exception e) { throw new RuntimeException(e); }
        intentRepository.save(intent);

        com.acme.payments.adapters.out.db.entity.ChargeEntity charge = new com.acme.payments.adapters.out.db.entity.ChargeEntity();
        try {
            java.lang.reflect.Field f;
            f = com.acme.payments.adapters.out.db.entity.ChargeEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(charge, "ch-" + req.orderId());
            f = com.acme.payments.adapters.out.db.entity.ChargeEntity.class.getDeclaredField("intent"); f.setAccessible(true); f.set(charge, intent);
            f = com.acme.payments.adapters.out.db.entity.ChargeEntity.class.getDeclaredField("amountMinor"); f.setAccessible(true); f.set(charge, amountMinor);
            f = com.acme.payments.adapters.out.db.entity.ChargeEntity.class.getDeclaredField("status"); f.setAccessible(true); f.set(charge, "CAPTURED");
            f = com.acme.payments.adapters.out.db.entity.ChargeEntity.class.getDeclaredField("settledAt"); f.setAccessible(true); f.set(charge, java.time.Instant.now());
        } catch (Exception e) { throw new RuntimeException(e); }
        chargeRepository.save(charge);

        try {
            java.lang.reflect.Field fo = com.acme.payments.adapters.out.db.entity.OrderEntity.class.getDeclaredField("status");
            fo.setAccessible(true);
            fo.set(order, "PAID");
            orderRepository.save(order);
        } catch (Exception ignored) {}
    }

    private void validateMoney(PaymentDtos.Money money) {
        if (!"INR".equals(money.currency())) {
            throw ApiException.unprocessable("CURRENCY_NOT_SUPPORTED", "Only INR is supported");
        }
        BigDecimal amt;
        try {
            amt = new BigDecimal(money.amount());
        } catch (NumberFormatException ex) {
            throw ApiException.unprocessable("AMOUNT_OUT_OF_RANGE", "Invalid amount format");
        }
        if (amt.scale() != 2) {
            throw ApiException.unprocessable("AMOUNT_OUT_OF_RANGE", "Amount must have two decimal places");
        }
        if (amt.compareTo(MIN) < 0 || amt.compareTo(MAX) > 0) {
            throw ApiException.unprocessable("AMOUNT_OUT_OF_RANGE", "Amount must be between 0.50 and 1,000,000.00 INR");
        }
    }
}
