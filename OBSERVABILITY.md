# Observability Plan

## Metrics
- Micrometer with Prometheus registry enabled via `build.gradle.kts` (`micrometer-registry-prometheus`).
- JVM/system binders registered in `MetricsConfiguration` (classloader, memory, GC, CPU, uptime).
- Business counters/timers in `PaymentCommandService`, `SubscriptionService`, `WebhookService`, settlement export pipeline, and queue requeue scheduler (`webhooks.requeued.count`).
- RabbitMQ queue depth exposed via Spring AMQP metrics (`spring.rabbitmq.listener.queues`) and consumed for alerting.

## Tracing
- Micrometer Observation/Tracing bridge with Brave+Zipkin reporter; spans wrap Authorize.Net REST calls, webhook persistence, and queue consumption.
- `ObservationRegistry` injected into outward-facing components (Authorize.Net HTTP client, payment/webhook services) to emit spans around external IO.
- Correlation ID filter integrates MDC for log correlation and is propagated into observations and queue messages.
- Sampling configured via `management.tracing.sampling.probability` in `application.yml` (default 10%).

## Logging
- Structured logging via SLF4J with correlation IDs enriched for every request and queue message (`WebhookQueueListener`).
- Error events captured with context in webhook processing, payment flows, settlement exports, and scheduler requeue attempts.

## Metrics Exposure
- `/actuator/metrics` and `/actuator/prometheus` exposed for scrapable metrics.
- `/actuator/trace` enabled for quick span diagnostics during development.
- RabbitMQ health surfaced via Spring AMQP `health` indicator and custom metric `webhooks.pending.count` (available via repository + meter binding).

## Next Steps
- Wire Zipkin/OTel exporter endpoints per environment once infra is provisioned.
- Add automated tests verifying presence of key counters/timers and span creation.
- Define dashboard/alerting expectations for payments latency, webhook backlog, export success ratio.

