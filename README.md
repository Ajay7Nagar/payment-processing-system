# Payment Processing System

Production-grade modular monolith for managing card payments, subscriptions, settlements, and compliance reporting on top of Authorize.Net.

## Highlights
- Single-tenant PostgreSQL schema managed via Flyway migrations.
- Authorize.Net REST integration with sandbox-ready configuration, signature validation, and queue-backed webhook ingestion.
- Comprehensive payment lifecycle (purchase, authorize/capture, cancel, refund) plus subscription management, dunning, and daily settlement exports.
- Immutable compliance audit log API with RBAC enforcement.
- Observability using Spring Boot Actuator, Micrometer metrics, distributed tracing via Micrometer Tracing + Zipkin, and structured logging with correlation IDs.
- ≥80% automated test coverage overall, ≥90% on critical payment paths.

## Architecture Overview
- **API Layer:** Spring Boot MVC controllers with RS256 JWT auth and role-based access (e.g., `PAYMENTS_WRITE`, `SETTLEMENT_EXPORT`, `COMPLIANCE_OFFICER`).
- **Domain Layer:** DDD-inspired aggregates for payments, subscriptions, settlements, and audit logs with optimistic locking and idempotency protection.
- **Persistence:** PostgreSQL with JDBC connection pooling (HikariCP); Flyway migrations under `src/main/resources/db/migration`.
- **Async Processing:** RabbitMQ-backed queues for webhook reconciliation and scheduled exports, with worker components under `application/workers`.
- **Observability:** Actuator `/metrics`, `/prometheus`, correlation ID filter, Micrometer counters/timers, tracing spans for gateway calls, structured JSON logging.

Detailed diagrams and design trade-offs are captured in `Architecture.md`.

## Getting Started

### Prerequisites
- Java 17 (Temurin recommended)
- Gradle Wrapper (bundled)
- Docker + Docker Compose (for local infrastructure)

### Environment Variables
Create a `.env` file or export environment variables before running containers:
```
AUTHORIZE_NET_LOGIN_ID=<sandbox login>
AUTHORIZE_NET_TRANSACTION_KEY=<sandbox key>
AUTHORIZE_NET_SIGNATURE_KEY=<hex signature key>
SECURITY_JWT_SECRET=<base64-encoded symmetric key for tests/dev>
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=5672
```

Local defaults are provided in `application.yml`; production secrets should be injected via Kubernetes secrets.

### Bootstrapping Locally
```bash
./gradlew clean build       # compile + run unit tests + Jacoco report
docker-compose --profile core up -d  # start Postgres and API container
```
Optional supporting services (RabbitMQ, worker) can be enabled with `--profile supporting`.

### Running the Application
- API service: `http://localhost:8080`
- Actuator endpoints: `http://localhost:8080/actuator`
- Metrics scrape: `http://localhost:8080/actuator/prometheus`

### Test Suite & Coverage
```bash
./gradlew clean test
open build/reports/tests/test/index.html        # Test results
open build/reports/jacoco/test/html/index.html  # Coverage report (>80% overall)
```
Highlights:
- Payment and subscription services >90% coverage.
- Compliance API, webhook queue workers, and settlement exports backed by dedicated unit tests.

## API Surface Summary
- `POST /api/v1/payments/purchase|authorize|capture|cancel|refund`
- `POST /api/v1/subscriptions` with lifecycle operations and dunning history endpoints.
- `POST /api/v1/settlement/export` (role: `SETTLEMENT_EXPORT`).
- `GET /api/v1/compliance/audit-logs` & `POST /api/v1/compliance/audit-logs/export` (role: `COMPLIANCE_OFFICER`).
- `POST /api/v1/webhooks/authorize-net` with signature validation and queue enqueue.

Complete OpenAPI document: `API-SPECIFICATION.yml` (generated manually for now).

## Observability & Ops
- Metrics configuration documented in `OBSERVABILITY.md`, including JVM binders, business counters, queue depth tracking, and tracing setup.
- Queue health monitored through RabbitMQ metrics and `WebhookQueueScheduler` requeue logic.
- `TEST_REPORT.md` contains the latest regression status and coverage summary.

## Deployment Notes
- Designed for Kubernetes deployment with separate API and worker pods.
- Use `Dockerfile.api` and `Dockerfile.worker` for container builds.
- Configure RabbitMQ connection details through environment variables per environment.

## Contributing
1. Fork and clone the repository.
2. Ensure tests pass locally with `./gradlew clean test`.
3. Update `TASKS.md` and relevant documentation for any new feature.
4. Submit PR including coverage evidence and documentation updates.

## Support & Contact
For production issues, escalate through the on-call runbook (to be integrated). For Authorize.Net sandbox credentials and operational overrides, coordinate with the payments platform team.
