# Delivery Tracks for Payment Processing System

The plan below sequences work into focused tracks that cover every functional and non-functional requirement from `requirements.md` and `raw-requirement.txt`. Each track builds on the previous one so the system reaches production readiness incrementally.

## Track 0 – Foundations & Enablement
- Bootstrap repo, Gradle, Spring Boot skeleton.
- Baseline observability hooks (correlation ID filter, structured logging scaffold).
- CI skeleton, coverage thresholds, coding standards.
- Containerisation (Dockerfiles, compose skeleton) to support later tracks.

## Track 1 – Core Purchase Flows (FR-1, FR-2, FR-3, FR-4, FR-11, FR-12)
- Model entities for orders, payments, refunds, audit logging.
- Implement Authorize.Net client (auth, capture, void, refund).
- REST endpoints: purchase (auth+capture), authorize, capture, cancel, refund.
- Persistence with idempotency keys for request replay safety.
- Error handling contract (structured codes/messages).
- Unit tests for service orchestration and gateway client stubs.

## Track 2 – Subscription & Recurring Billing (FR-5, FR-6)
- Subscription domain model (plans, schedules, trials, proration).
- Endpoints for subscription lifecycle (create, pause, resume, cancel).
- Recurring charge scheduler + retry policy (0d,1d,3d,7d, auto-cancel at 30d).
- Persistence linking subscriptions to transactions.
- Tests covering schedule calculations and retry/backoff logic.

## Track 3 – Async Webhooks & Queue Processing (FR-6, FR-7, FR-8, FR-14, NFR-6)
- Webhook ingestion endpoints (payment, refund, subscription events).
- Verify Authorize.Net signatures, deduplicate via idempotency store.
- Queue-backed worker (Redis/Kafka) draining webhook events asynchronously.
- Metrics for queue depth, error rates, retry outcomes.
- Integration tests with simulated webhook payloads and idempotency clashes.

## Track 4 – Security, Compliance & Isolation (FR-8, FR-9, FR-10, FR-13, FR-17, NFR-3, NFR-4, NFR-5, NFR-9)
- JWT-based RBAC (HS256/RS256) enforcing merchant scope on endpoints.
- Data partitioning/filters per merchant for multi-tenant isolation.
- Immutable audit log persistence and exposure for compliance officers.
- Secrets management guidance, PCI DSS considerations, rate limiting policy.
- Redaction/anonymisation flows for GDPR requests.
- Tests: security filters, compliance API, isolation rules.

## Track 5 – Observability, Tracing, Metrics (FR-14, NFR-7, FR-8 extension)
- Correlation ID propagation across API, worker, external calls.
- Structured logging schema & trace IDs in logs.
- Micrometer metrics (latency, throughput, queue stats, retries).
- `/metrics`, `/health`, `/info` exposure with Docker profile config.
- Documentation in `OBSERVABILITY.md`, example Kibana/PromQL queries.

## Track 6 – Reporting & Settlement Exports (FR-15, NFR-8)
- Daily settlement export job (CSV/JSON) per merchant with batch IDs.
- Storage integration (local S3 mock or filesystem).
- API/download endpoint for compliance teams.
- Tests verifying export correctness and scheduling.

## Track 7 – Acceptance, Packaging & Deliverables (All remaining NFRs & docs)
- Acceptance test suites covering AT-1…AT-10 scenarios.
- Finalise API specification, README, Architecture, PROJECT_STRUCTURE docs.
- Populate TEST_REPORT.md, TESTING_STRATEGY.md, compliance notes.
- Capture screenshots, record demo video per deliverables list.
- Validate `docker-compose up -d` launches full stack with profiles.

Each track should conclude with merged code, passing CI, updated docs, and tagged release before proceeding to the next. This roadmap ensures sequential, end-to-end delivery aligned with the stated requirements.
