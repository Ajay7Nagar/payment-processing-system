package com.example.payments.adapters.messaging;

import com.example.payments.domain.webhook.WebhookEvent;
import com.example.payments.infra.messaging.RabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public WebhookEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(WebhookEvent event) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.WEBHOOK_EXCHANGE, RabbitMqConfig.ROUTING_KEY, event.getId());
    }
}
