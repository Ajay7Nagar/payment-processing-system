# Architecture Overview

## Problem Framing
- Build a single-tenant payment processing backend integrating Authorize.Net sandbox.
- Support core/advanced payment flows, recurring billing, idempotency, webhooks, and compliance logging.
- Meet `requirements.md` for SLA (99.9%), latency (p95 <300 ms), GDPR/SOC2 readiness, and ≥80% automated test coverage.

## Chosen Pattern
- **Modular Monolith with Async Workers** deployed on Kubernetes.
- Single deployable core for REST APIs, domain services, and shared data layer.
- Asynchronous worker modules consuming queues for webhooks, retries, and exports.

## Component Responsibilities
- **API Gateway Layer**: Handles JWT auth, rate limiting, request validation, correlation IDs, REST endpoints.
- **Domain Modules**: Encapsulate payment flow logic (purchase, refund, subscription lifecycle, dunning) with idempotent services.
- **Persistence Layer**: Central database storing transactions, subscriptions, settlements, and audit logs.
- **Async Worker Pool**: Processes Authorize.Net webhooks (RabbitMQ queue), retry schedules, daily settlement exports, and queue drains.
- **Observability Module**: Provides structured logging, tracing hooks, metrics endpoint, and audit trail access for compliance.

```text
+---------------------------+       +---------------------+
|        Clients            |       | Authorize.Net API   |
+-------------+-------------+       +----------+----------+
              |                                |
              v                                v
      +-------------------+            +--------------+
      | API Gateway Layer |<---------->| Async Worker |
      +---------+---------+    queue   +------+-------+
                |                         ^   |
                v                         |   |
      +-------------------+               |   |
      | Domain Modules    |---------------+   |
      +---------+---------+                   |
                |                             |
                v                             |
      +-------------------+                   |
      | Persistence Layer |<------------------+
      +-------------------+
```

## Data Flows (High-Level)
1. **Synchronous Requests**: Client → API Gateway → Domain Modules → Persistence → Authorize.Net (for live calls) → response with correlation ID.
2. **Webhook Intake**: Authorize.Net → API endpoint (signature validation) → persist payload → publish event ID to RabbitMQ → worker consumes → Domain Modules update state → Metrics/Audit logging.
3. **Recurring Billing & Dunning**: Scheduler enqueues jobs → Async Worker executes retries → updates Persistence and emits logs.
4. **Settlement Export**: End-of-day job enqueues export task → Worker generates CSV/JSON → stores in S3-compatible bucket or exposes via API.

## Security Considerations
- RS256 JWT validation with role-based checks.
- All data globally scoped; no tenant-specific context required.
- Secrets managed via Kubernetes secrets; Authorize.Net API keys centrally configured.
- Logs and traces redact PII; GDPR erasure triggers soft-delete/anonymization within 24 hours.

## Performance Targets
- API latency p95 <300 ms, p99 <500 ms excluding gateway time; worker backlog drained <2 minutes for 100 events/minute bursts.
- Support horizontal scaling of API pods and worker replicas; staging mirrors production quotas within ±5%.

## Failure Handling Strategy
- Idempotency keys ensure safe retries; duplicate webhook events ignored via persisted replay table.
- Circuit breakers and retry policies on Authorize.Net calls; degraded mode returns graceful errors.
- Queue backpressure monitored via metrics; DLQs and manual replay tooling for stuck messages.
- Health probes on API and worker processes for Kubernetes rolling updates and auto-restart.

## Local Validation Plan
- `docker-compose` stack with API app, Postgres, RabbitMQ broker, and mock S3.
- Sandbox credentials configured via `.env`; run integration tests against Authorize.Net sandbox.
- Load test critical flows locally (e.g., k6) to verify latency targets and idempotent behavior.
- Use webhook simulator to inject payment/refund events and confirm queue drain metrics.

## Technology Options and Trade-offs

### API Gateway Layer (Spring Boot)
- **Recommended: Spring Boot MVC + Spring Security + Spring Validation**
  - Pros: Production-proven, mature ecosystem, straightforward synchronous programming model, excellent tooling, integrates cleanly with Actuator.
  - Cons: Blocking IO requires tuning thread pools for high concurrency.

### Domain & Application Modules
- **Recommended: Spring Boot modular packages with Spring Data JPA**
  - Pros: Clear module boundaries, annotation-driven transactions, supports PostgreSQL features (JSONB), widely supported testing libraries.
  - Cons: Requires diligence to avoid tight coupling across packages.

### Persistence Layer
- **Recommended: PostgreSQL (managed in prod, Dockerized locally)**
  - Pros: Strong transactional guarantees, JSONB for webhook payload storage, broad managed service support.
  - Cons: Needs partitioning strategy and tuning for high write throughput.

### Messaging / Async Processing
- **Recommended: RabbitMQ with Spring AMQP**
  - Pros: Durable queues, DLQs, routing flexibility, proven in fintech workloads, simple Docker image for local dev.
  - Cons: Slightly heavier than Redis, but operationally mature.

### Observability & Auditing
- **Recommended: Spring Boot Actuator + Micrometer + Prometheus + Grafana**
  - Pros: First-class Spring integration, metrics and health endpoints out-of-the-box, Prometheus/Grafana easy to containerize locally, fits Kubernetes.
  - Cons: Requires maintaining Grafana dashboards (manageable for core metrics).

### Secrets & Config
- **Recommended**: Spring Cloud Config (optional) + Kubernetes secrets in prod; `.env`/Docker Compose for local.

### Local Test & Simulation Tooling
- **Recommended: Node.js + Express webhook simulator (containerized)**
  - Pros: Minimal footprint, rapid scripting for webhook/dunning scenarios, easily bundled into Docker Compose.
  - Cons: Requires Node.js runtime but trivial to manage.

## Updated Technology Evaluation Matrix

| Component | Recommended Option | Security | Reliability | Scalability | Resilience | Performance | Maintainability | Local Complexity |
|-----------|--------------------|----------|-------------|-------------|------------|-------------|-----------------|------------------|
| API Layer | Spring Boot MVC + Spring Security | High | High | Medium-High | High | High | High | Low |
| Domain Modules | Spring Boot + Spring Data JPA | High | High | Medium | High | High | High | Low |
| Persistence | PostgreSQL | High | High | Medium-High | High | High | High | Medium |
| Queue | RabbitMQ + Spring AMQP | High | High | Medium-High | High | High | Medium | Medium |
| Observability | Actuator + Micrometer + Prometheus/Grafana | High | High | High | High | High | Medium | Medium |
| Secrets Config | K8s Secrets + Env files (local) | High | High | High | High | High | High | Low |
| Simulator | Node.js Express | Medium | Medium | Medium | Medium | Medium | High | Low |

*Primary choices balance production-grade robustness with manageable local setup; alternatives remain viable if constraints change.*

## Detailed System Design

### Module Decomposition
- **Auth & Access Module (Spring Security)**: Validates RS256 JWTs, enriches MDC with correlation IDs, enforces role-based access (operator, compliance, service).
- **Payment Orchestration Module**: Handles purchase, authorize/capture, cancel, refund flows. Uses Spring Data JPA aggregates (`PaymentOrder`, `PaymentTransaction`) with optimistic locking for idempotency. Invokes Authorize.Net through a Spring-managed HTTP client abstraction that calls the REST API in non-test profiles and swaps to fast mocks in tests. Responses are wrapped into domain events and persisted alongside audit logs.
- **Subscription & Billing Module**: Manages subscription entities, schedules, proration credits, and dunning pipelines. Publishes retry jobs to RabbitMQ and records timeline entries for compliance.
- **Webhook Module**: Provides signed webhook endpoint, validates Authorize.Net signature headers, persists payloads (`WebhookEvent`), deduplicates via event hash, and triggers downstream reconciliations.
- **Reporting & Settlement Module**: Generates daily exports, exposes operator dashboards via REST, aggregates metrics, and tracks settlement batch IDs.
- **Compliance & Audit Module**: Writes append-only audit records (`AuditLog`) with immutable hashes, supports soft-delete/anonymization workflows, and serves compliance API queries with pagination and filtering.
- **Platform Services Module**: Cross-cutting support for tracing (OpenTelemetry bridge), feature toggles, scheduler (Spring Task + Kubernetes CronJobs), and configuration binding.

### Subscriptions & Recurring Billing

- `SubscriptionService` orchestrates lifecycle transitions, retry backoff, and integrates with Authorize.Net purchases using idempotency keys.
- `SubscriptionScheduler` processes due subscriptions and triggers billing worker logic.
- `SubscriptionController` exposes REST endpoints for create, retrieve, update, pause/resume/cancel, schedule listing, and dunning history with RBAC enforced via `@PreAuthorize`.
- Persistence relies on `subscriptions`, `subscription_schedules`, and `dunning_attempts` tables.

### Request Lifecycle (Example: Purchase Flow)
1. Client sends `POST /api/v1/payments/purchase` with JWT and idempotency key.
2. Spring MVC controller validates payload, logs correlation ID.
3. Orchestration service checks idempotency table; if new, persists pending transaction and calls Authorize.Net through gateway adapter.
4. Response mapped to internal status (settled/pending), transaction record updated, audit entry written, metrics incremented.
5. Controller returns API response with correlation ID; logs and traces emitted via Micrometer/OTel bridge.

### Data Model Overview (PostgreSQL)
- `customers` (`id`, `external_ref`, `pii_hash`, `status`, `created_at`).
- `payment_orders` (`id`, `customer_id`, `amount`, `currency`, `status`, `correlation_id`, `idempotency_key`, `request_id`, `created_at`, `updated_at`).
- `payment_transactions` (`id`, `order_id`, `type`, `amount`, `authnet_transaction_id`, `status`, `processed_at`).
- `subscriptions` (`id`, `customer_id`, `plan_code`, `billing_cycle`, `interval_days`, `trial_end`, `status`, `client_reference`, `next_billing_at`).
- `subscription_schedules` (`id`, `subscription_id`, `attempt_number`, `scheduled_at`, `status`, `failure_reason`).
- `dunning_attempts` (`id`, `subscription_id`, `scheduled_at`, `status`, `failure_code`, `failure_message`).
- `webhook_events` (`id`, `event_id`, `event_type`, `signature`, `payload`, `received_at`, `status`, `dedupe_hash`).
- `settlement_exports` (`id`, `batch_id`, `generated_at`, `location_uri`, `status`).
- `audit_logs` (`id`, `actor`, `operation`, `resource_type`, `resource_id`, `metadata`, `created_at`).

### Messaging & Job Design (RabbitMQ)
- **Exchange** `webhook.events` (direct) → queue `webhook.events.queue` (DLQ: `webhook.events.dlq`).
  - `webhook.events.queue`: Consumed by `WebhookQueueListener`; payload contains webhook event ID persisted in Postgres.
  - DLQ is monitored and re-queued by `WebhookQueueScheduler` for stale `PROCESSING` events.
- Future queues (e.g., billing retry, settlement exports) follow similar patterns with dedicated listeners and DLQs.

### Configuration & Secrets Management
- Application properties externalized via Spring profiles (`dev`, `test`, `staging`, `prod`).
- Sensitive credentials (Authorize.Net keys, DB passwords, JWT public keys) injected via Kubernetes secrets; local Compose reads from `.env` file avoided in version control.
- Feature flags managed through configuration table or environment variables with refresh endpoints guarded by admin role.

### Deployment Topology (Kubernetes)
- **API Deployment**: Spring Boot container, horizontal pod autoscaler (HPA) targeting CPU 60% and custom latency metric; minimum 2 replicas.
- **Worker Deployment**: Separate Spring Boot worker image (shared codebase, different profile) consuming RabbitMQ queues; scaled via queue depth metric.
- **RabbitMQ StatefulSet**: Single node in lower envs, clustered in prod with quorum queues.
- **PostgreSQL**: Managed service (e.g., AWS RDS) with read replica for analytics; local Compose uses single container.
- **Prometheus & Grafana**: Deployed via Helm chart; scrape API/Worker metrics and RabbitMQ exporter.
- **CronJobs**: Kubernetes CronJobs trigger settlement generation and maintenance tasks (e.g., PII anonymization sweeps).

### Observability & Alerting
- Metrics: Request latency/throughput, webhook queue depth, dunning success rate, settlement export time, JWT auth failures, DB connection pool usage.
- Logging: JSON structured logs with correlation IDs, sanitized payload references; shipped to centralized log store (e.g., ELK) with retention per policy.
- Tracing: Spring Sleuth / OpenTelemetry instrumentation capturing external Authorize.Net calls and RabbitMQ spans.
- Alerts: SLA breach (API latency), queue backlog > threshold for >5 minutes, dunning retry failures >X%, database error rate spikes, webhook signature failures.

### Failure & Recovery Scenarios
- **Authorize.Net outage**: Circuit breaker opens, responses include retry-after guidance, backlog queued for later replay.
- **Queue backpressure**: HPA scales worker pods; if DLQ grows, ops alerted to inspect failed events.
- **Database failover**: Application uses connection retries; RDS multi-AZ handles primary promotion; long-running transactions minimized.
- **Stuck CronJob**: Jobs idempotent; reruns safe; status tracked in `settlement_exports` table for manual intervention.

### Testing & Quality Gates
- **Unit Tests**: JUnit + Mockito for domain services, covering success/failure paths, achieving ≥80% coverage.
- **Integration Tests**: Testcontainers for PostgreSQL/RabbitMQ; contract tests with Authorize.Net sandbox stubs.
- **Performance Tests**: k6 scripts hitting purchase/refund endpoints under concurrency; verify latency SLOs.
- **Resilience Tests**: Chaos scripts simulate Authorize.Net failure, queue backlog, worker crash; ensure graceful degradation.
- **Compliance Checks**: Automated scans confirming audit log immutability and PII anonymization workflows.

### Local Development Experience
- `docker-compose.yml` starts API (dev profile), Postgres, RabbitMQ, Prometheus, Grafana, webhook simulator, and mock S3 (e.g., MinIO).
- Makefile or Gradle tasks for migrating schema (Flyway), running tests, generating coverage report, seeding dev data.
- CLI scripts to publish sample webhooks, trigger settlement export, and inspect audit log API responses.

### Scaling & Capacity Planning
- Baseline sizing: API pods 2 vCPU/4 GB RAM; worker pods 1 vCPU/2 GB RAM; RabbitMQ 1 vCPU/2 GB RAM in staging.
- Estimate throughput: 20 concurrent API calls → ~100 RPS peak; RabbitMQ sized for 6k messages/hour bursts.
- Growth path: Increase API replicas, partition tables by time, introduce read replicas for reporting, and consider extracting subscription module if isolation needed later.

### API Surface

- `/api/v1/subscriptions` for lifecycle management (create, retrieve, update, pause, resume, cancel, list).
- `/api/v1/subscriptions/{subscriptionId}/schedules` to inspect generated billing schedules.
- `/api/v1/subscriptions/{subscriptionId}/dunning` to view retry history and outcomes.
- `/api/v1/payments/*` endpoints for purchase, authorize, capture, cancel, refund, and retrieval.
