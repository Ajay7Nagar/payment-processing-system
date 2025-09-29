package com.acme.payments.bootapi.payments;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/payments")
public class PaymentsController {

    private final PaymentsService service;
    private final com.acme.payments.bootapi.idempotency.IdempotencyService idempotencyService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public PaymentsController(PaymentsService service,
                              com.acme.payments.bootapi.idempotency.IdempotencyService idempotencyService,
                              com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.service = service;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/purchase")
    public ResponseEntity<?> purchase(@Validated @RequestBody PaymentDtos.PurchaseRequest request,
                                      @RequestHeader(name = "X-Idempotency-Key", required = false) String idemKey) {
        String merchantId = "test-merchant"; // placeholder until JWT wiring provides merchant_id
        if (idemKey != null && !idemKey.isBlank()) {
            String scope = idempotencyService.scopeKey(merchantId, "/v1/payments/purchase", idemKey);
            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(request);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                payloadJson = request.toString();
            }
            String payloadHash = idempotencyService.hashPayload(payloadJson);
            String cached = idempotencyService.findResponse(scope, payloadHash);
            if (cached != null) {
                return ResponseEntity.status(201).build();
            }
            service.validatePurchase(request);
            service.createOrderIfAbsent(request);
            PaymentDtos.Payment payment = toPayment(request.orderId(), request.amount());
            idempotencyService.storeResponse(scope, payloadHash, toJson(payment));
            return ResponseEntity.status(201).body(payment);
        } else {
            service.validatePurchase(request);
            service.createOrderIfAbsent(request);
            PaymentDtos.Payment payment = toPayment(request.orderId(), request.amount());
            return ResponseEntity.status(201).body(payment);
        }
    }

    @PostMapping("/authorize")
    public ResponseEntity<?> authorize(@Validated @RequestBody PaymentDtos.AuthorizeRequest request) {
        // stub
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/capture")
    public ResponseEntity<?> capture(@Validated @RequestBody PaymentDtos.CaptureRequest request) {
        // stub
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(@Validated @RequestBody PaymentDtos.CancelRequest request,
                                    @RequestHeader(name = "X-Idempotency-Key", required = false) String idemKey) {
        String merchantId = "test-merchant";
        if (idemKey != null && !idemKey.isBlank()) {
            String scope = idempotencyService.scopeKey(merchantId, "/v1/payments/cancel", idemKey);
            String payloadJson;
            try { payloadJson = objectMapper.writeValueAsString(request); } catch (com.fasterxml.jackson.core.JsonProcessingException e) { payloadJson = request.toString(); }
            String payloadHash = idempotencyService.hashPayload(payloadJson);
            String cached = idempotencyService.findResponse(scope, payloadHash);
            if (cached != null) {
                return ResponseEntity.ok().build();
            }
            // stub call to service layer would go here
            PaymentDtos.CancelResponse respDto = new PaymentDtos.CancelResponse(request.authorizationId(), "CANCELED");
            idempotencyService.storeResponse(scope, payloadHash, toJson(respDto));
            return ResponseEntity.ok(respDto);
        } else {
            // stub call to service layer would go here
            return ResponseEntity.ok(new PaymentDtos.CancelResponse(request.authorizationId(), "CANCELED"));
        }
    }

    private PaymentDtos.Payment toPayment(String id, PaymentDtos.Money money) {
        return new PaymentDtos.Payment(id, money, "CAPTURED", java.time.Instant.now().toString());
    }

    private String toJson(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }
}
