package com.example.payments.adapters.api.payments;

import com.example.payments.adapters.api.payments.dto.AuthorizeRequest;
import com.example.payments.adapters.api.payments.dto.CancelRequest;
import com.example.payments.adapters.api.payments.dto.CaptureRequest;
import com.example.payments.adapters.api.payments.dto.PaymentResponse;
import com.example.payments.adapters.api.payments.dto.PurchaseRequest;
import com.example.payments.adapters.api.payments.dto.RefundRequest;
import com.example.payments.application.services.PaymentCommandService;
import com.example.payments.domain.payments.Money;
import com.example.payments.domain.shared.CorrelationId;
import com.example.payments.domain.shared.IdempotencyKey;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Validated
public class PaymentsController {

    private final PaymentCommandService paymentCommandService;

    public PaymentsController(PaymentCommandService paymentCommandService) {
        this.paymentCommandService = paymentCommandService;
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasAuthority('PAYMENTS_PURCHASE_CREATE')")
    public ResponseEntity<PaymentResponse> purchase(@Valid @RequestBody PurchaseRequest request) {
        var order = paymentCommandService.purchase(request.customerId(),
                new Money(request.amount(), request.currency()), request.paymentNonce(),
                new IdempotencyKey(request.idempotencyKey()), new CorrelationId(request.correlationId()),
                request.requestId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(order));
    }

    @PostMapping("/authorize")
    @PreAuthorize("hasAuthority('PAYMENTS_AUTHORIZE_CREATE')")
    public ResponseEntity<PaymentResponse> authorize(@Valid @RequestBody AuthorizeRequest request) {
        var order = paymentCommandService.authorize(request.customerId(),
                new Money(request.amount(), request.currency()), request.paymentNonce(),
                new IdempotencyKey(request.idempotencyKey()), new CorrelationId(request.correlationId()),
                request.requestId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(order));
    }

    @PostMapping("/capture")
    @PreAuthorize("hasAuthority('PAYMENTS_CAPTURE_EXECUTE')")
    public ResponseEntity<PaymentResponse> capture(@Valid @RequestBody CaptureRequest request) {
        var order = paymentCommandService.capture(request.orderId(),
                new Money(request.amount(), "USD"), request.actorId());
        return ResponseEntity.ok(toResponse(order));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAuthority('PAYMENTS_CANCEL_EXECUTE')")
    public ResponseEntity<PaymentResponse> cancel(@Valid @RequestBody CancelRequest request) {
        var order = paymentCommandService.cancel(request.orderId(), request.actorId());
        return ResponseEntity.ok(toResponse(order));
    }

    @PostMapping("/refund")
    @PreAuthorize("hasAuthority('PAYMENTS_REFUND_EXECUTE')")
    public ResponseEntity<Void> refund(@Valid @RequestBody RefundRequest request) {
        paymentCommandService.refund(request.orderId(),
                new Money(request.amount(), "USD"), request.lastFour(), request.actorId());
        return ResponseEntity.accepted().build();
    }

    private PaymentResponse toResponse(com.example.payments.domain.payments.PaymentOrder order) {
        return new PaymentResponse(order.getId(), order.getCustomerId(), order.getMoney().amount(),
                order.getMoney().currency(), order.getStatus(), order.getCorrelationId());
    }
}
