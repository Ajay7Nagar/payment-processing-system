package com.acme.payments.bootapi.payments;

import com.acme.payments.adapters.out.db.entity.ChargeEntity;
import com.acme.payments.adapters.out.db.entity.OrderEntity;
import com.acme.payments.adapters.out.db.entity.PaymentIntentEntity;
import com.acme.payments.adapters.out.db.repo.ChargeRepository;
import com.acme.payments.adapters.out.db.repo.OrderRepository;
import com.acme.payments.adapters.out.db.repo.PaymentIntentRepository;
import com.acme.payments.bootapi.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPaymentsOpsService implements PaymentsOpsService {

    private final OrderRepository orderRepository;
    private final PaymentIntentRepository intentRepository;
    private final ChargeRepository chargeRepository;

    private final com.acme.payments.adapters.out.gateway.AuthorizeNetClient gateway;

    public DefaultPaymentsOpsService(OrderRepository orderRepository,
                                     PaymentIntentRepository intentRepository,
                                     ChargeRepository chargeRepository,
                                     com.acme.payments.adapters.out.gateway.AuthorizeNetClient gateway) {
        this.orderRepository = orderRepository;
        this.intentRepository = intentRepository;
        this.chargeRepository = chargeRepository;
        this.gateway = gateway;
    }

    @Override
    public void authorize(PaymentDtos.AuthorizeRequest req) {
        OrderEntity order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> ApiException.unprocessable("NOT_FOUND", "Order not found"));
        PaymentIntentEntity intent = new PaymentIntentEntity();
        try {
            java.lang.reflect.Field f;
            f = PaymentIntentEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(intent, "pi-" + req.orderId());
            f = PaymentIntentEntity.class.getDeclaredField("order"); f.setAccessible(true); f.set(intent, order);
            f = PaymentIntentEntity.class.getDeclaredField("type"); f.setAccessible(true); f.set(intent, "AUTHORIZE");
            f = PaymentIntentEntity.class.getDeclaredField("status"); f.setAccessible(true);
            com.acme.payments.adapters.out.gateway.AuthorizeNetClient.GatewayResponse gr = gateway.authorize(req.paymentToken(), new java.math.BigDecimal(req.amount().amount()).movePointRight(2).longValueExact(), req.amount().currency());
            if (!gr.approved) throw new ApiException("GATEWAY_DECLINED", "Authorize declined", HttpStatus.BAD_GATEWAY);
            f.set(intent, "AUTHORIZED");
            f = PaymentIntentEntity.class.getDeclaredField("gatewayRef"); f.setAccessible(true); f.set(intent, gr.referenceId);
        } catch (Exception e) { throw new RuntimeException(e); }
        intentRepository.save(intent);
    }

    @Override
    public void capture(PaymentDtos.CaptureRequest req) {
        PaymentIntentEntity intent = intentRepository.findById(req.authorizationId())
                .orElseThrow(() -> ApiException.unprocessable("NOT_FOUND", "Authorization not found"));
        ChargeEntity charge = new ChargeEntity();
        try {
            java.lang.reflect.Field f;
            f = ChargeEntity.class.getDeclaredField("id"); f.setAccessible(true); f.set(charge, "ch-" + req.authorizationId());
            f = ChargeEntity.class.getDeclaredField("intent"); f.setAccessible(true); f.set(charge, intent);
            long amountMinor = req.amount() == null ? 0L : new BigDecimal(req.amount().amount()).movePointRight(2).longValueExact();
            f = ChargeEntity.class.getDeclaredField("amountMinor"); f.setAccessible(true); f.set(charge, amountMinor);
            com.acme.payments.adapters.out.gateway.AuthorizeNetClient.GatewayResponse gr = gateway.capture(req.authorizationId(), amountMinor);
            if (!gr.approved) throw new ApiException("GATEWAY_DECLINED", "Capture declined", HttpStatus.BAD_GATEWAY);
            f = ChargeEntity.class.getDeclaredField("status"); f.setAccessible(true); f.set(charge, "CAPTURED");
        } catch (Exception e) { throw new RuntimeException(e); }
        chargeRepository.save(charge);
        try {
            java.lang.reflect.Field fs = PaymentIntentEntity.class.getDeclaredField("status"); fs.setAccessible(true); fs.set(intent, "CAPTURED");
        } catch (Exception ignored) {}
        intentRepository.save(intent);
    }

    @Override
    @Transactional
    public PaymentDtos.CancelResponse cancel(PaymentDtos.CancelRequest req) {
        PaymentIntentEntity intent = intentRepository.findById(req.authorizationId())
                .orElseThrow(() -> ApiException.unprocessable("NOT_FOUND", "Authorization not found"));

        try {
            java.lang.reflect.Field fs = PaymentIntentEntity.class.getDeclaredField("status"); fs.setAccessible(true);
            Object currentStatus = fs.get(intent);

            if ("CAPTURED".equals(currentStatus)) {
                throw new ApiException("CONFLICT", "Cannot void a captured transaction", HttpStatus.CONFLICT);
            }
            if ("CANCELED".equals(currentStatus)) {
                return new PaymentDtos.CancelResponse(intent.getId(), "CANCELED");
            }

            com.acme.payments.adapters.out.gateway.AuthorizeNetClient.GatewayResponse gr = gateway.voidAuth(intent.getGatewayRef());

            if (!gr.approved) {
                throw new ApiException("GATEWAY_DECLINED", "Void declined by gateway", HttpStatus.BAD_GATEWAY);
            }

            fs.set(intent, "CANCELED");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        intentRepository.save(intent);
        return new PaymentDtos.CancelResponse(intent.getId(), "CANCELED");
    }
}
