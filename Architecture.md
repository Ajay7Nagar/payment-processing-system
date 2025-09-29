# Architecture – Payment Processing System

## 1. Problem framing
Build a robust backend integrating with Authorize.Net Sandbox to support purchase, authorize/capture, cancel(void), refunds (full/partial), subscriptions/recurring billing, idempotent retries, and async webhook handling. System must enforce JWT/RBAC, idempotency, distributed tracing, metrics, error catalog, rate limits, INR-only v1, and documented compliance/observability.

## 2. Chosen pattern
Monolith API + external queue (Redis Streams) + separate worker process. Single database for persistence (orders, payments, refunds, subscriptions, webhooks, idempotency, outbox, audit), Redis for durable queueing/backpressure, and a dedicated worker service for async tasks and webhook/event processing.

## 3. Components and responsibilities
- API Service (Monolith)
  - Exposes REST endpoints: Purchase, Authorize, Capture, Cancel, Refund, Subscription Management, Listings, Webhook receiver.
  - Validates JWT (RS256), RBAC, and tenant scoping via `merchant_id` claim.
  - Enforces currency/amount rules, BIN allow/deny lists, and per-merchant limits.
  - Writes orders/transactions and idempotency records; returns standardized errors with correlation_id.
  - Produces outbox records and enqueues tasks to Redis Streams for async work.
  - Exposes metrics endpoint and structured logs with correlation_id.
- Worker Service
  - Consumes Redis Streams, processes async work (e.g., webhook events, subscription charges, dunning retries).
  - Calls Authorize.Net, applies idempotency, optimistic locking, and state transitions.
  - Writes audit logs, updates orders/transactions, emits events to outbox when needed.
  - Implements poison-queue handling after bounded retries.
- PostgreSQL 15
  - Source of truth: entities for Merchant, Customer (token only), Order, PaymentIntent, Charge, Refund, Subscription, Invoice, WebhookEvent (raw+normalized), IdempotencyRecord, OutboxEvent, AuditLog.
  - Constraints for invariants (e.g., Σ(refunds) ≤ original, unique idempotency scope, versioned rows).
- Redis Streams
  - Durable queue for async processing; consumer groups for horizontal worker scaling.
  - Backpressure and replay support; DLQ/poison stream after failure threshold.
- OpenTelemetry Collector (local)
  - Aggregates traces/metrics/logs for demo; aligns with tracing requirement.

## 4. Data flows
```
+----------------+        +-----------------+        +-------------------+
|  Client / CLI  | -----> |     API Svc     | -----> |   PostgreSQL 15   |
+----------------+        +-----------------+        +-------------------+
          |                        |   |                       ^
          |  Webhook (HTTP)  ----> |   | enqueue tasks         |
          v                        v   |                       |
+----------------+        +-----------------+        +-------------------+
| Authorize.Net  | -----> |  Webhook Rx/API |  -->  |   Redis Streams   |
+----------------+        +-----------------+        +-------------------+
                                                    |
                                                    v
                                            +---------------+
                                            |  Worker Svc   |
                                            +---------------+
                                                    |
                                                    v
                                               +---------+
                                               |  DB     |
                                               +---------+
```

Flow highlights:
- Sync API paths (purchase/auth/capture/cancel/refund) validate/JWT/limits → DB write → gateway call (when sync) → respond with correlation_id and standardized errors.
- Webhooks: API verifies HMAC + clock skew → persist raw event → enqueue → 200 OK (<200 ms) → worker processes with idempotency and updates DB.
- Subscriptions/dunning: scheduled tasks enqueued; worker performs retries T+0,+1d,+3d,+7d and updates status.

## 5. Security considerations
- JWT/RBAC: RS256, claims: iss, sub, aud, iat, exp, merchant_id, roles; tenant scoping on all queries.
- Webhook authenticity: HMAC verification, 5m clock skew limit, current+previous key (90‑day rotation).
- Idempotency: scope `{merchant_id}:{endpoint}:{key}`, TTL 24h, payload hash ≤10 KB.
- Secrets: env locally, vault/KMS in non-dev; no secrets in VCS; rotation every 90 days.
- PCI: SAQ‑A, no PAN/PII/PCI data stored; tokenization only; log scrubbing.
- Rate limiting: per merchant/IP budgets; 429 with Retry‑After.

## 6. Performance targets (from requirements)
- API availability 99.9%/month.
- p95 latency: POST /payments/purchase < 300 ms; GET /transactions < 200 ms.
- Webhook ack p95 < 200 ms; ingestion→durable enqueue p95 < 150 ms; end‑to‑end webhook processing p95 < 2 s.
- Throughput: API 300 RPS sustained (burst 600 RPS 1 min). Workers: queue latency p95 < 1 s, max in‑flight per merchant = 5.

## 7. Failure handling strategy
- Duplicate/out‑of‑order events: 7‑day dedupe store (event ID + signature hash); idempotent handlers.
- Concurrency conflicts: optimistic locking on order versions; capture vs refund → capture wins if auth open; non‑settled refund → 409 CAPTURE_PENDING.
- Gateway instability: exponential backoff with bounded retries; circuit breaker; classify errors (timeout, declined, error).
- Queue/worker outages: persist webhook in DB before enqueue; replay on recovery; poison stream after 10 failures for manual inspection.
- Merchant counter contention: serialized updates on per‑merchant counters with transactional checks.
- Time skew/signature drift: reject skew > 5m; maintain two active HMAC keys during rotation.

## 8. Local validation plan
- Compose stack: API, Worker, PostgreSQL 15, Redis Streams, OpenTelemetry Collector, MailHog, webhook replayer.
- Contract tests: Authorize.Net sandbox flows with deterministic fixtures; record/replay for CI.
- Performance tests: k6 scenarios for POST purchase and webhook pipeline to verify p95 budgets and throughput.
- Property-based tests: refund invariants (Σ(refunds) ≤ original), subscription dunning schedule.
- Chaos toggles: inject gateway 5xx/timeouts, duplicate webhooks, network jitter, DB deadlocks.
- Security checks: secrets scan, JWT verification tests, log scrubbing (no PAN/PII/PCI).

## 9. Technology options and evaluation

Design goal: production-grade quality with a simple, reliable local setup. Prefer defaults that run well in prod and are easy to emulate locally.

The following options fit the chosen pattern (Monolith API + Redis Streams + Worker) and keep the app minimal. Primary stack favors Java 24 + Spring Boot.

### 9.1 API and Worker Services
- Spring Boot 3.x (Java 24)
  - Pros: Mature ecosystem, Spring Security/JWT, Micrometer/OTel integration, scheduling, validation, Testcontainers.
  - Cons: Footprint higher than micro frameworks; cold start slower than Quarkus.
- Micronaut (Java)
  - Pros: Fast startup/low memory; annotation processing reduces reflection.
  - Cons: Smaller ecosystem; less out-of-the-box than Spring for payments domain.
- Quarkus (Java)
  - Pros: Very fast startup; good OTel support; modern stack.
  - Cons: Learning curve; Spring compatibility layer not perfect.

Evaluation (API/Worker)
| Option | Security | Reliability | Performance | Dev productivity | Ecosystem | Local demo complexity |
|---|---|---|---|---|---|---|
| Spring Boot 3 | High | High | High | High | High | Low |
| Micronaut | High | High | High | Medium | Medium | Low |
| Quarkus | High | High | High | Medium | Medium | Medium |

### 9.2 Persistence (PostgreSQL 15)
- Hibernate ORM + Spring Data JPA
  - Pros: Rapid CRUD; transaction management; validation; optimistic locking.
  - Cons: Hidden N+1/query tuning; learning curve for advanced mappings.
- jOOQ
  - Pros: Type-safe SQL; excellent for complex queries/invariants.
  - Cons: More verbose for CRUD; license considerations for some editions.
- MyBatis
  - Pros: Control over SQL; predictable performance.
  - Cons: Boilerplate; fewer conveniences than JPA.

Evaluation (Persistence)
| Option | Query power | Performance | Maintainability | Learning curve | Fit with Spring |
|---|---|---|---|---|---|
| Spring Data JPA | Medium | High | High | Medium | High |
| jOOQ | High | High | Medium | Medium-High | Medium |
| MyBatis | High | High | Medium | Medium | Medium |

### 9.3 Migrations
- Flyway
  - Pros: Simple, widely used; integrates with Spring Boot.
  - Cons: SQL-only migrations by default.
- Liquibase
  - Pros: Declarative changelogs; diff generation.
  - Cons: Heavier; steeper learning curve.

### 9.4 Queueing (Redis Streams)
- Redis Streams (server) + Lettuce (Java client)
  - Pros: Native Streams/group consumers; good performance; lightweight for local demo.
  - Cons: At-least-once semantics; need idempotent handlers.
- Redis Streams + Redisson
  - Pros: Higher-level abstractions; resilience utilities.
  - Cons: Extra dependency, licensing for some features.

Evaluation (Queueing)
| Option | Reliability | Scalability | Performance | Operational simplicity | Ecosystem |
|---|---|---|---|---|---|
| Streams + Lettuce | High | High | High | High | High |
| Streams + Redisson | High | High | High | Medium | Medium |

### 9.5 HTTP/Gateway client
- Spring WebClient (reactive)
  - Pros: Non-blocking; built-in backpressure; good for high concurrency.
  - Cons: Reactive model adds complexity.
- Apache HttpClient (blocking)
  - Pros: Mature; straightforward; sufficient for moderate RPS.
  - Cons: Thread-per-request model; more resources under load.
- OkHttp
  - Pros: Efficient; HTTP/2 support; simple API.
  - Cons: Extra integration work vs Spring defaults.

### 9.6 Security/JWT and RBAC
- Spring Security + Nimbus JOSE/JWT (RS256)
  - Pros: Standards-based; robust JWT validation; method/URL security; RBAC.
  - Cons: Configuration heavy; needs careful tuning.

### 9.7 Observability
- Micrometer + OpenTelemetry Java Agent/SDK
  - Pros: Unified metrics/traces; easy Prometheus exposition; correlation IDs.
  - Cons: Must curate labels/cardinality; agent tuning.
- Logback JSON + MDC
  - Pros: Structured logs with correlation_id, merchant_id, order_id.
  - Cons: Requires log processing in prod (out of scope locally).

Evaluation (Observability)
| Option | Tracing | Metrics | Ease of setup | Overhead | Local demo fit |
|---|---|---|---|---|---|
| Micrometer + OTel | High | High | High | Low-Med | High |
| Logback JSON + MDC | Medium | N/A | High | Low | High |

### 9.8 Rate limiting
- Bucket4j (in-memory/Redis backend)
  - Pros: Mature; token bucket; integrates with Spring; Redis support for distributed limits.
  - Cons: Must size Redis ops carefully at high RPS.

### 9.9 Testing & load tools
- JUnit 5 + Mockito + Testcontainers (Postgres/Redis)
  - Pros: Realistic integration tests; ephemeral infra; ≥80%/≥90% critical-path coverage.
  - Cons: Slower than pure unit tests.
- k6 (load testing)
  - Pros: Scriptable; good for latency/RPS/SLO checks.
  - Cons: Extra tooling to learn.

### 9.10 Webhook simulator (any language)
- Node.js (Express) replayer
  - Pros: Quick to write; broad community; easy JSON tooling.
  - Cons: Another runtime to manage.
- Go CLI replayer
  - Pros: Single binary; high performance.
  - Cons: Requires Go toolchain if building locally.
- Python (Flask) replayer
  - Pros: Minimal code; good for scripting.
  - Cons: Virtualenv management; slower than Go.

Evaluation (Simulator)
| Option | Simplicity | Performance | Ecosystem | Local setup |
|---|---|---|---|---|
| Node.js | High | Medium | High | Medium |
| Go | Medium | High | Medium | Medium |
| Python | High | Low-Med | High | Medium |

### 9.11 Recommended minimal set (keeps approach unchanged)
- API/Worker: Spring Boot 3 (Java 24), Spring Security, Spring Validation, Scheduling.
- Persistence: PostgreSQL 15, Spring Data JPA, Flyway.
- Queue: Redis Streams with Lettuce client.
- HTTP client: Spring WebClient (or Apache HttpClient if simpler).
- Observability: Micrometer + OpenTelemetry Java Agent, Logback JSON with MDC.
- Rate limiting: Bucket4j (optionally backed by Redis for distributed limits).
- Testing: JUnit 5, Mockito, Testcontainers; k6 for load.
- Simulator: Node.js or Go webhook replayer.


### 9.12 Pragmatic recommendations (prod-ready, simple locally)
- API/Worker: Spring Boot 3 (Java 24) with Spring Security, Validation, Micrometer. Reason: mature, observable, easy to hire for; great test support.
- Persistence: PostgreSQL 15, Spring Data JPA (with explicit fetch plans) + Flyway. Reason: balances productivity and control; Flyway is simple.
- Queue: Redis Streams with Lettuce client. Reason: lightweight, good enough for burst handling and exactly-once via idempotency at handlers.
- HTTP client: Spring WebClient for async calls; use Apache HttpClient if team prefers blocking simplicity (toggle via profile).
- Observability: Micrometer + OpenTelemetry Java Agent; Logback JSON + MDC. Reason: meets tracing/metrics/logging NFRs with minimal code.
- Rate limiting: Bucket4j with Redis backend in prod; local default in-memory. Reason: simple to run locally, scalable in prod.
- Testing: JUnit 5, Mockito, Testcontainers; k6 for load. Reason: realistic, repeatable; CI-friendly.
- Simulator: Node.js webhook replayer locally; optional Go binary for CI speed. Reason: quick start locally; fast in CI if needed.

Trade-offs consciously accepted
- At-least-once deliveries from Redis Streams handled by idempotent consumers. Avoids heavier brokers.
- JPA chosen for speed; for hot paths or complex queries, selectively use jOOQ.
- WebClient is default for backpressure; blocking client acceptable behind worker threads if simpler for team.
- Redis-backed Bucket4j only in prod to keep local friction low.


### 9.12 Pragmatic recommendations (prod-ready, simple locally)
- API/Worker: Spring Boot 3 (Java 24) with Spring Security, Validation, Micrometer. Reason: mature, observable, easy to hire for; great test support.
- Persistence: PostgreSQL 15, Spring Data JPA (with explicit fetch plans) + Flyway. Reason: balances productivity and control; Flyway is simple.
- Queue: Redis Streams with Lettuce client. Reason: lightweight, good enough for burst handling and exactly-once via idempotency at handlers.
- HTTP client: Spring WebClient for async calls; use Apache HttpClient if team prefers blocking simplicity (toggle via profile).
- Observability: Micrometer + OpenTelemetry Java Agent; Logback JSON + MDC. Reason: meets tracing/metrics/logging NFRs with minimal code.
- Rate limiting: Bucket4j with Redis backend in prod; local default in-memory. Reason: simple to run locally, scalable in prod.
- Testing: JUnit 5, Mockito, Testcontainers; k6 for load. Reason: realistic, repeatable; CI-friendly.
- Simulator: Node.js webhook replayer locally; optional Go binary for CI speed. Reason: quick start locally; fast in CI if needed.

Trade-offs consciously accepted
- At-least-once deliveries from Redis Streams handled by idempotent consumers. Avoids heavier brokers.
- JPA chosen for speed; for hot paths or complex queries, selectively use jOOQ.
- WebClient is default for backpressure; blocking client acceptable behind worker threads if simpler for team.
- Redis-backed Bucket4j only in prod to keep local friction low.

## 10. System Design

### 10.1 Domain model (high-level)
- Merchant: merchant_id, name, limits (per_txn/daily/monthly), bin_allow[], bin_deny[], features.ach (bool), created_at.
- Customer: customer_id, merchant_id, email/name (PII), gateway_token (no PAN), created_at.
- Order: order_id, merchant_id, amount_in_minor (INR), currency=INR, status, version, created_at.
- PaymentIntent: intent_id, order_id, type (PURCHASE|AUTHORIZE), status (AUTHORIZED|CAPTURED|CANCELED|FAILED), gateway_ref, correlation_id, created_at.
- Charge: charge_id, intent_id, amount_in_minor, settled_at, status (SETTLED|PENDING|FAILED).
- Refund: refund_id, charge_id, amount_in_minor, status (REQUESTED|COMPLETED|FAILED), created_at.
- Subscription: subscription_id, customer_id, schedule (MONTHLY|WEEKLY|EVERY_N_DAYS), n_days, trial_days, billing_day, status (ACTIVE|PAST_DUE|PAUSED|CANCELED), next_run_at.
- Invoice: invoice_id, subscription_id, period_start/end, amount_due_minor, credit_applied_minor, status (OPEN|PAID|FAILED).
- WebhookEvent: event_id, vendor_event_id, type (normalized), received_at, raw_json, signature_hash, processed_at, correlation_id.
- IdempotencyRecord: scope_key, request_hash, response_snapshot, created_at, expires_at.
- OutboxEvent: outbox_id, aggregate_type, aggregate_id, event_type, payload, published (bool), created_at.
- AuditLog: audit_id, actor, action, resource, correlation_id, created_at.

### 10.2 State machines (essential)
- PaymentIntent: NEW → AUTHORIZED → CAPTURED → (terminal) or NEW → FAILED (terminal) or AUTHORIZED → CANCELED (terminal).
- Refund: REQUESTED → COMPLETED (terminal) or REQUESTED → FAILED (terminal).
- Subscription: NEW → ACTIVE → (on failures) PAST_DUE → (after 14d) PAUSED → (after 30d) CANCELED; ACTIVE → CANCELED (manual).

### 10.3 API surface (aligned to FR IDs)
- POST /v1/payments/purchase  [FR-1]
- POST /v1/payments/authorize  [FR-2]
- POST /v1/payments/capture    [FR-2]
- POST /v1/payments/cancel     [FR-3]
- POST /v1/payments/refund     [FR-4]
- POST /v1/subscriptions       [FR-5]
- POST /v1/subscriptions/{id}/cancel  [FR-5]
- GET  /v1/transactions        (cursor pagination, filters) [FR-17]
- POST /v1/webhooks/authorize-net  [FR-7]
- GET  /v1/metrics             [NFR-2]
- All protected endpoints require JWT (RS256) with merchant_id and roles claims [FR-9].

### 10.4 Idempotency and concurrency
- Scope: `{merchant_id}:{endpoint}:{key}`; TTL 24h; store payload hash ≤10 KB and response snapshot.
- Duplicates return original response (HTTP 200/201 or same error), no side effects [FR-6].
- Optimistic locking on versioned rows (order, intent, charge); retries on 409 conflicts [FR-19].

### 10.5 Webhook pipeline
- Verify HMAC, signature window ≤5 minutes, accept current/previous secrets; reject otherwise.
- Persist raw event + normalized type, compute dedupe key (vendor_event_id + signature_hash), store if new.
- Enqueue to Redis Streams; respond 200 to gateway within <200 ms p95 [NFR-4].
- Worker consumes, enforces idempotency (7-day dedupe window), updates domain state, writes audit.
- Poison stream after 10 failures for manual inspection.

### 10.6 Subscriptions and dunning
- Schedules: MONTHLY, WEEKLY, EVERY_N_DAYS; optional trial_days; billing_day with end-of-month rule.
- Dunning attempts at T+0, +1d, +3d, +7d (max 4). After final failure → PAST_DUE; pause at 14d; auto-cancel at 30d (configurable).
- Mid-cycle plan change: apply proration credit to account balance; next invoice consumes credit first.

### 10.7 Rate limits
- Authenticated per-merchant: 200 RPS sustained, burst 500 RPS for 60s; unauth endpoints: 20 RPS.
- Enforce with Bucket4j; Redis backend in prod, in-memory locally; return 429 + Retry-After [NFR-8].

### 10.8 Observability (metrics, logs, traces)
- Metrics (labels: endpoint, merchant, status): request rate, p50/p95/p99 latency, error rates; gateway latency/outcomes; webhook queue depth/processing/dead-letter; idempotency replay count; subscription outcome by attempt.
- Traces: correlation_id propagated across API → DB → queue → worker; export via OpenTelemetry.
- Logs: structured JSON (correlation_id, merchant_id, order_id, event_id); no PAN/PII/PCI; retention per env.

### 10.9 Error model and mapping
- 400 INVALID_REQUEST, 401 UNAUTHORIZED, 403 FORBIDDEN, 404 NOT_FOUND.
- 409 CONFLICT (e.g., CAPTURE_PENDING, remaining balance violation), 422 BIN_BLOCKED/AMOUNT_OUT_OF_RANGE/CURRENCY_NOT_SUPPORTED.
- 429 RATE_LIMITED (with Retry-After).
- 5xx: GATEWAY_TIMEOUT/GATEWAY_DECLINED/GATEWAY_ERROR with correlation_id.

### 10.10 Configuration (env-driven)
- Core: APP_ENV, SERVER_PORT, CURRENCY=INR, FEATURES_ACH=false, SCA_PROVIDER=disabled.
- DB: POSTGRES_URL or POSTGRES_HOST/PORT/DB/USER/PASSWORD; FLYWAY_ENABLED=true.
- Redis: REDIS_URL or REDIS_HOST/PORT; STREAM_NAMES (defaults).
- Auth/JWT: JWT_ISSUERS, JWT_AUDIENCE, JWT_JWKS_URL or JWT_PUBLIC_KEY.
- Webhooks: WEBHOOK_SECRET_CURRENT, WEBHOOK_SECRET_PREVIOUS, WEBHOOK_CLOCK_SKEW_MAX=300s.
- Gateway: ANET_API_LOGIN_ID, ANET_TRANSACTION_KEY (provided via secrets).
- Rate limits: RATE_LIMIT_AUTH_RPS, RATE_LIMIT_BURST_RPS, RATE_LIMIT_PUBLIC_RPS.

### 10.11 Scalability and capacity planning
- API: stateless; horizontal scale for RPS; tune connection pools; prefer WebClient for backpressure.
- Workers: scale consumer group size to keep queue latency p95 < 1 s; cap in-flight per merchant = 5.
- DB: use optimistic locking; hot queries with indexes; consider partitioning by merchant if needed.
- Redis: size stream retention; monitor consumer lag; DLQ for poison events.

### 10.12 Local setup (simple, production-aligned)
- docker-compose services: api (placeholder), worker (placeholder), postgres:15, redis:latest, otel-collector, mailhog, webhook-replayer.
- Defaults: in-memory Bucket4j locally; Redis-backed in prod; same endpoints/headers/log formats across envs.
- CI: Testcontainers for Postgres/Redis; record/replay sandbox fixtures; generate `TEST_REPORT.md` and coverage artifacts.

