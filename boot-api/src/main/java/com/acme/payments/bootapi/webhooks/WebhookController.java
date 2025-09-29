package com.acme.payments.bootapi.webhooks;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/webhooks/authorize-net")
public class WebhookController {
    private final WebhookService service;
    private final com.acme.payments.adapters.out.queue.EventQueue queue;
    private final io.micrometer.core.instrument.Counter webhookEnqueuedCounter;

    public WebhookController(WebhookService service,
                             com.acme.payments.adapters.out.queue.EventQueue queue,
                             io.micrometer.core.instrument.Counter webhookEnqueuedCounter) {
        this.service = service;
        this.queue = queue;
        this.webhookEnqueuedCounter = webhookEnqueuedCounter;
    }

    @PostMapping
    public ResponseEntity<?> receive(@RequestBody String payload,
                                     @RequestHeader(name = "X-Signature", required = false) String signature,
                                     @RequestHeader(name = "X-Timestamp", required = false) Long epochSeconds) {
        long ts = epochSeconds == null ? 0L : epochSeconds;
        service.verify(payload, signature, ts);
        queue.enqueue("webhooks.authorize_net", payload);
        webhookEnqueuedCounter.increment();
        return ResponseEntity.ok().build();
    }
}
