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
public class RefundsController {

    private final RefundsService refundsService;
    private final com.acme.payments.bootapi.idempotency.IdempotencyService idempotencyService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public RefundsController(RefundsService refundsService,
                             com.acme.payments.bootapi.idempotency.IdempotencyService idempotencyService,
                             com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.refundsService = refundsService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/refund")
    public ResponseEntity<?> refund(@Validated @RequestBody RefundDtos.RefundRequest request,
                                    @RequestHeader(name = "X-Idempotency-Key", required = false) String idemKey) {
        String merchantId = "test-merchant";
        if (idemKey != null && !idemKey.isBlank()) {
            String scope = idempotencyService.scopeKey(merchantId, "/v1/payments/refund", idemKey);
            String payloadJson;
            try { payloadJson = objectMapper.writeValueAsString(request); } catch (com.fasterxml.jackson.core.JsonProcessingException e) { payloadJson = request.toString(); }
            String payloadHash = idempotencyService.hashPayload(payloadJson);
            String cached = idempotencyService.findResponse(scope, payloadHash);
            if (cached != null) {
                return ResponseEntity.status(201).build();
            }
            refundsService.validateRefundRequest(request);
            refundsService.createRefund(request);
            idempotencyService.storeResponse(scope, payloadHash, "{}");
            return ResponseEntity.status(201).build();
        } else {
            refundsService.validateRefundRequest(request);
            refundsService.createRefund(request);
            return ResponseEntity.status(201).build();
        }
    }
}
