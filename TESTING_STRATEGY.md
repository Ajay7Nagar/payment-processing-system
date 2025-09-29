# Testing Strategy

Scope: Ensure functional correctness and production-grade non-functional behavior for the Monolith API + Redis Streams + Worker architecture. Tests map to FR/NFR IDs in `requirements.md` and use environment-based configuration.

## 1. Test layers and objectives
- Unit (JUnit 5)
  - Target: fast validation of domain policies and state machines.
  - Focus: amount/bounds (FR-15), BIN allow/deny (FR-16), idempotency key scope (FR-6), state transitions for payment/refund/subscription (Section 10.2 in Architecture).
- Property-based (JQwik)
  - Target: invariants across ranges and sequences.
  - Focus: Σ(refunds) ≤ original (FR-4), subscription schedules and dunning windows (FR-5), pagination cursors (FR-17).
- Integration (Testcontainers: PostgreSQL, Redis)
  - Target: persistence mappings, optimistic locking (FR-19), idempotency/outbox behavior, Redis Streams consumer groups and dedupe (FR-6/7).
- Contract tests (Authorize.Net sandbox)
  - Target: gateway requests/responses and error mapping (FR-10/12); webhook authenticity checks (FR-7).
  - Note: credentials via env; no secrets in repo. Replay deterministic scenarios via fixtures.
- End-to-end flows (local compose)
  - Target: purchase, auth→capture, cancel, full/partial refunds, subscription create + simulated charges via webhook replayer; JWT/RBAC checks (FR-1..5, FR-7..9, FR-12, FR-17).
- Non-functional (k6 + metrics)
  - Target: latency/throughput, rate limits, webhook ack and e2e budgets, resilience. See `nonfunctional-test-plan.md`.

## 2. Coverage goals and quality bars
- Line coverage ≥ 80% overall; ≥ 90% on critical paths (purchase, auth→capture, refund, subscription charge, webhook ingest) [NFR-3].
- Error model conformance across all endpoints; standardized schema with correlation_id [FR-12].
- Zero sensitive data in logs; 100% responses with correlation_id [NFR-1, Security].

## 3. Test data and fixtures
- Deterministic amounts within INR rules (FR-14/15); valid/invalid BIN samples for allow/deny.
- Webhook payload fixtures for: payment.authorized|captured|settled|failed, refund.completed, subscription.* events (FR-7).
- Idempotency: repeated identical requests with the same `X-Idempotency-Key`.

## 4. Tooling
- Unit/Integration: JUnit 5, Testcontainers (PostgreSQL 15, Redis), JQwik for properties.
- Load/Performance: k6 scenarios aligned to acceptance thresholds in `nonfunctional-test-plan.md`.
- Coverage reporting: JaCoCo; snapshot summarized in `TEST_REPORT.md`.

## 5. Execution (local)
- Unit + integration: run the Maven test phase with Testcontainers enabled; ensure DB/Redis start per-test and are isolated.
- Contract tests: run against Authorize.Net sandbox with env-provided credentials; record outcomes for screenshots.
- End-to-end: bring up docker-compose (API, Worker, DB, Redis, OTel, MailHog, replayer); run smoke and flow suites; verify DB and sandbox screenshots.
- Load tests: execute k6 scripts for purchase, transactions listing, webhook ack/e2e; capture metrics and logs.

## 6. CI gates
- Fail build if: coverage < 80% overall or < 90% on critical-path packages; any nonconforming error schema; any secrets detected in logs/artifacts.
- Upload artifacts: JaCoCo report, k6 summary, logs with correlation IDs, `TEST_REPORT.md`.

## 7. Traceability
- Map each test suite and scenario to FR/NFR IDs; include IDs in test display names and in `TEST_REPORT.md` tables for quick auditing.

## 8. Reporting
- Update `TEST_REPORT.md` after each CI run with: coverage %, pass/fail by suite, latency percentiles, error rates, and any DLQ/duplicate counts.
- Attach two screenshots: DB transactions view, Authorize.Net sandbox view, showing matching transactions (including a subscription charge and a webhook-driven update).
