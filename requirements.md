# Requirements – Payment Processing System

Scope: Single service integrating with Authorize.Net Sandbox to support payment flows. In-scope methods: card-not-present credit/debit (baseline). Optional via flag: ACH/eCheck when `features.ach=true`. Digital wallets out of scope for v1.

## 1. Functional Requirements (FR)
1. [FR-1] Purchase – authorize and capture in one step.
   - [AT-FR-1.1] Given a valid card and amount, Purchase creates one Authorize.Net transaction; DB stores a successful payment with capture details.
   - [AT-FR-1.2] For invalid input, API returns structured 4xx; no success record persisted.
2. [FR-2] Authorize then Capture – two-step flow.
   - [AT-FR-2.1] Authorize records an uncaptured auth; Capture with auth reference settles and stores capture.
   - [AT-FR-2.2] Capture on invalid/already-captured auth returns error; no duplicate capture.
3. [FR-3] Cancel (void) before capture.
   - [AT-FR-3.1] Cancel on uncaptured auth voids authorization; status recorded as canceled.
   - [AT-FR-3.2] Cancel on captured auth returns error indicating not applicable.
4. [FR-4] Refunds – full and partial.
   - [AT-FR-4.1] Refund full on settled charge creates and stores refund.
   - [AT-FR-4.2] Partial refunds allowed; Σ(refunds) ≤ original amount enforced.
5. [FR-5] Subscriptions / Recurring Billing.
   - [AT-FR-5.1] Supports Monthly, Weekly, and Custom every N days; optional trial (N days); specific billing day with end-of-month rule.
   - [AT-FR-5.2] Dunning: attempts at T+0, +1d, +3d, +7d (max 4). After final failure: status PAST_DUE; pause at 14d; auto-cancel at 30d (configurable). Events recorded.
6. [FR-6] Idempotency & Retries for client requests and webhooks.
   - [AT-FR-6.1] Idempotency key scope `{merchant_id}:{endpoint}:{key}`, TTL 24h; payload hash ≤10 KB compared; duplicates return original response.
   - [AT-FR-6.2] Webhook dedupe window 7d using gateway event ID + signature hash; duplicates have no side effects.
7. [FR-7] Webhooks – async events handling.
   - [AT-FR-7.1] Handle: payment.authorized|captured|settled|failed, refund.completed, subscription.created|updated|canceled|charge.succeeded|charge.failed; map to internal enum.
   - [AT-FR-7.2] Authenticity: HMAC verified, clock-skew ≤5m, event unprocessed; else 401/400; accepted events are enqueued and 200 returned.
8. [FR-8] Endpoints exposed for each action.
   - [AT-FR-8.1] `API-SPECIFICATION.yml` documents Purchase, Authorize, Capture, Cancel, Refund, Subscription ops, Webhook receiver.
   - [AT-FR-8.2] Valid requests return 2xx with schema; invalid return standardized 4xx/5xx.
9. [FR-9] Authentication and authorization (JWT, RBAC, tenancy).
   - [AT-FR-9.1] JWT issued by our Auth (RS256) with claims: iss, sub, aud, iat, exp, merchant_id, roles; missing/invalid → 401.
   - [AT-FR-9.2] Roles: admin, ops, read_only; data access scoped by merchant_id claim; forbidden access → 403.
10. [FR-10] Authorize.Net sandbox integration via API key/official SDK.
    - [AT-FR-10.1] With valid sandbox credentials, gateway calls succeed; with invalid/missing, calls fail with clear error codes.
    - [AT-FR-10.2] No third-party all-in-one wrapper used.
11. [FR-11] Persistence of orders and transaction history.
    - [AT-FR-11.1] Each flow (purchase, capture, cancel, refund, subscription charge) persists records with audit fields (time, actor, correlation_id).
    - [AT-FR-11.2] Order query reflects latest payment state including webhook updates.
12. [FR-12] Standardized error responses.
    - [AT-FR-12.1] Error body includes code, message, correlation_id; no sensitive data.
    - [AT-FR-12.2] Error codes catalog includes: INVALID_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, RATE_LIMITED, BIN_BLOCKED, AMOUNT_OUT_OF_RANGE, CURRENCY_NOT_SUPPORTED, GATEWAY_TIMEOUT, GATEWAY_DECLINED, GATEWAY_ERROR, IDEMPOTENCY_REPLAY, WEBHOOK_SIGNATURE_INVALID, WEBHOOK_DUPLICATE.
13. [FR-13] Payment methods scope & feature flags.
    - [AT-FR-13.1] Card CNP flows enabled by default; ACH/eCheck only when `features.ach=true`.
    - [AT-FR-13.2] Digital wallets endpoints absent in v1.
14. [FR-14] Currency handling (INR only in v1).
    - [AT-FR-14.1] Currency must be INR; minor_units=2; amounts formatted with scale=2.
    - [AT-FR-14.2] Config guardrails prevent multi-currency unless explicitly enabled; requests with other currencies → 422 CURRENCY_NOT_SUPPORTED.
15. [FR-15] Order amount rules and merchant limits.
    - [AT-FR-15.1] Server enforces 0.50 ≤ amount ≤ 1,000,000.00 INR; out-of-range → 422 AMOUNT_OUT_OF_RANGE.
    - [AT-FR-15.2] Optional per-merchant caps (per_txn, daily, monthly) enforced transactionally; over-limit → 409 CONFLICT.
16. [FR-16] BIN/Brand allow/deny lists per merchant.
    - [AT-FR-16.1] Evaluate before gateway calls; deny wins over allow; blocked BIN → 422 BIN_BLOCKED.
17. [FR-17] Listing and search semantics.
    - [AT-FR-17.1] Cursor-based pagination with `next_cursor`; default limit=50, max=200.
    - [AT-FR-17.2] Filters: status, date range, merchant, amount range, card brand, subscription_id; sorting by created_at (default desc), amount, status.
18. [FR-18] API versioning.
    - [AT-FR-18.1] All endpoints under URL `/v1/...`; responses include `X-API-Version: v1`.
19. [FR-19] Concurrency conflict resolution on same order.
    - [AT-FR-19.1] Use versioned updates; capture vs refund race: if auth open, capture wins; refund against non-settled returns 409 CAPTURE_PENDING.
    - [AT-FR-19.2] Concurrent refunds enforce remaining balance atomically; excess attempt → 409 CONFLICT.
20. [FR-20] Extensibility for SCA.
    - [AT-FR-20.1] Provide a pluggable SCA provider extension point; v1 excludes 3DS/SCA.

## 2. Non-Functional Requirements (NFR)
1. [NFR-1] Distributed tracing and correlation.
   - Target: 100% API responses carry correlation_id; all gateway calls/logs include it.
   - [AT-NFR-1.1] Any request → response has correlation header; logs share same ID end-to-end, including webhooks.
2. [NFR-2] Metrics endpoint.
   - Target: Expose metrics with counters, latency, error rates per endpoint/merchant/status.
   - [AT-NFR-2.1] Hitting metrics endpoint returns 200 and includes throughput, p50/p95/p99, error rates, webhook queue depth, dead-letter size, idempotency replays, subscription attempt outcomes.
3. [NFR-3] Test coverage.
   - Target: ≥80% line overall; ≥90% on critical paths (purchase, auth-capture, refund, subscription charge, webhook ingest).
   - [AT-NFR-3.1] Coverage report meets thresholds; CI artifact stored in `TEST_REPORT.md`.
4. [NFR-4] Webhook responsiveness and processing.
   - Target: Ack (enqueue) p95 < 200 ms; ingestion→durable enqueue p95 < 150 ms; end-to-end processing p95 < 2 s.
   - [AT-NFR-4.1] Load test shows budgets met; duplicates processed once.
5. [NFR-5] Availability and latency SLOs.
   - Target: API availability 99.9%/month. p95 latency: POST /payments/purchase < 300 ms; GET /transactions < 200 ms.
   - [AT-NFR-5.1] SLO dashboards show compliance over rolling month.
6. [NFR-6] Secrets management and rotation.
   - Target: No secrets in VCS; env vars locally; vault/KMS for staging/prod; rotation every 90 days.
   - [AT-NFR-6.1] Repo scan finds no credentials; runtime reads from env/secret store; rotation logs show two active keys (current+previous).
7. [NFR-7] Error model quality.
   - Target: Standardized error schema across endpoints; no sensitive data in messages.
   - [AT-NFR-7.1] Contract tests validate schema (code, message, correlation_id) for 4xx/5xx.
8. [NFR-8] Rate limiting and abuse controls.
   - Target: Authenticated per-merchant 200 RPS sustained, bursts 500 RPS for 60s; unauth endpoints 20 RPS; 429 with Retry-After.
   - [AT-NFR-8.1] Load tests trigger limits and observe 429 behavior with headers.
9. [NFR-9] Scalability throughput and workers.
   - Target: API 300 RPS sustained, 600 RPS burst 1 min; webhook workers start N=10/pod, autoscale to keep queue latency p95 < 1 s; max in-flight per merchant = 5.
   - [AT-NFR-9.1] Stress tests meet throughput and queue latency targets.
10. [NFR-10] Logging policy and retention.
   - Target: No PAN/PII/PCI data in logs; include correlation_id, merchant_id, order_id, event_id; prod retention 30 days, lower envs 7 days.
   - [AT-NFR-10.1] Log scrubs verified by tests; retention configured per environment.
11. [NFR-11] Data retention and privacy.
   - Target: PII retained 2 years; transactional records 7 years; webhook raw payloads 30 days; soft-delete + anonymization job (retain token and last4 only).
   - [AT-NFR-11.1] Policies enforced via jobs; verification queries confirm windows.
12. [NFR-12] Idempotency and outbox retention.
   - Target: Idempotency and outbox records retained 90 days with periodic purge.
   - [AT-NFR-12.1] Purge job reduces old records; queries show compliance.
13. [NFR-13] Environments and time standards.
   - Target: Envs: dev, test, staging, prod (staging mirrors prod flags/quotas); all timestamps UTC ISO-8601 (Z).
   - [AT-NFR-13.1] APIs return timestamps in UTC ISO-8601; staging config matches prod defaults.
14. [NFR-14] API versioning signals.
   - Target: All responses include `X-API-Version` and paths use `/v1`.
   - [AT-NFR-14.1] Contract tests validate headers and URL structure.
15. [NFR-15] Local compose and dependencies (developer UX).
   - Target: `docker-compose.yml` includes PostgreSQL 15, Redis (streams), MailHog, Webhook relay simulator, and OpenTelemetry Collector for tracing demo.
   - [AT-NFR-15.1] `docker compose up` starts all services and health checks pass.
16. [NFR-16] PCI scope and SCA extensibility.
   - Target: SAQ-A: no card data stored/processed on our servers; hosted fields/tokenization only. SCA provider is pluggable; v1 disabled.
   - [AT-NFR-16.1] Compliance review confirms no PAN/PII persistence; SCA toggle present and off by default.

Traceability: FR/NFRs derive from `raw-requirement.txt` sections 1–2 and provided clarifications. IDs are stable for test mapping.
