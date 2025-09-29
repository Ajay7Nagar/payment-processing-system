package com.acme.payments.bootapi.subscriptions;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/v1/subscriptions")
public class SubscriptionsController {

    private final SubscriptionsService service;

    public SubscriptionsController(SubscriptionsService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@Validated @RequestBody SubscriptionDtos.CreateRequest req) {
        service.validateCreate(req);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable("id") String id) {
        service.validateCancel(id);
        return ResponseEntity.ok().build();
    }
}


