package com.example.payments.infra.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@Configuration
@EnableRabbit
@Profile("!test")
public class RabbitMqConfig {

    public static final String WEBHOOK_EXCHANGE = "webhook.events";
    public static final String WEBHOOK_QUEUE = "webhook.events.queue";
    public static final String WEBHOOK_DLQ = "webhook.events.dlq";
    public static final String ROUTING_KEY = "authorize.net";

    @Bean
    public Declarables webhookBindings() {
        DirectExchange exchange = new DirectExchange(WEBHOOK_EXCHANGE, true, false);
        Queue queue = QueueBuilder.durable(WEBHOOK_QUEUE)
                .withArgument("x-dead-letter-exchange", WEBHOOK_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", WEBHOOK_DLQ)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(WEBHOOK_DLQ).build();
        DirectExchange dlx = new DirectExchange(WEBHOOK_EXCHANGE + ".dlx", true, false);
        return new Declarables(
                exchange,
                queue,
                dlx,
                deadLetterQueue,
                BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY),
                BindingBuilder.bind(deadLetterQueue).to(dlx).with(WEBHOOK_DLQ));
    }

    @Bean
    public ConnectionFactory rabbitConnectionFactory(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password) {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(WEBHOOK_EXCHANGE);
        template.setRoutingKey(ROUTING_KEY);
        return template;
    }
}
