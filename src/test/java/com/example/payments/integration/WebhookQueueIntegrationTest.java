package com.example.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.payments.adapters.persistence.WebhookEventRepository;
import com.example.payments.domain.webhook.WebhookEvent;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class WebhookQueueIntegrationTest {

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Test
    void eventShouldBePersisted() {
        WebhookEvent event = WebhookEvent.create("evt-integration", "PAYMENT_SETTLED", "{}", "sig", "hash",
                OffsetDateTime.now());

        WebhookEvent saved = webhookEventRepository.save(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(webhookEventRepository.findById(saved.getId())).isPresent();
    }
}
