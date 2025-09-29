# Non-functional Test Plan – Payment Processing System

Scope: Local, production-aligned validation of performance, scalability, resilience, reliability, security (non-PII logging), and observability. Targets and scenarios are derived from requirements.md and Architecture.md.

## 1. Environment (local)
- docker-compose services: API, Worker, PostgreSQL 15, Redis (Streams), OpenTelemetry Collector, MailHog, Webhook Replayer.
- Configuration: INR only; JWT enabled; rate limits per `requirements.md`; idempotency TTL 24h; webhook dedupe window 7d.
- Data: Seed a small catalog of test customers/tokens and fixed amounts to achieve deterministic results.

## 2. Metrics & evidence to collect
- Latency: p50/p95/p99 by endpoint; end-to-end for webhook processing.
- Throughput: requests/sec by endpoint; queue processing rate; consumer lag.
- Error rates: by error code (4xx/5xx); rate-limit counters.
- Queue health: webhook queue depth, DLQ size, retry counts.
- Idempotency: replay count; duplicate suppression count.
- Logging: correlation_id presence; absence of PAN/PII/PCI data.
- Artifacts: TEST_REPORT.md summary; screenshots (transactions in DB and Authorize.Net), k6 output, logs with correlation IDs.

## 3. Acceptance thresholds (local)
- API availability (test window): ≥ 99% successful responses during load.
- p95 latency: POST /v1/payments/purchase < 300 ms; GET /v1/transactions < 200 ms.
- Webhook ack: HTTP 200 within p95 < 200 ms; ingestion→durable enqueue p95 < 150 ms (from metrics/log timestamps).
- Webhook end-to-end: event received → state updated p95 < 2 s under nominal load; 0 duplicate side effects for duplicate deliveries.
- Throughput: sustain 300 RPS API for 5 min; burst 600 RPS for 60 s without systemic failures; queue latency p95 < 1 s with N=10 workers.
- Rate limiting: 429 with Retry-After when limits exceeded; < 1% 5xx under configured loads.
- Error model: all errors return standardized schema (code, message, correlation_id).
- Security/logging: 0 occurrences of PAN/PII/PCI in logs; correlation_id present in 100% of responses.

## 4. Scenarios
1) Purchase latency and throughput (FR-1, NFR-5)
- Load: Ramp to 300 RPS sustained for 5 min; 600 RPS burst for 60 s.
- Measure: p50/p95/p99 latency, success rate, error codes; CPU/memory of API; DB/Redis utilization.
- Accept: p95 < 300 ms; success ≥ 99%; < 1% 5xx.

2) Transactions listing latency (FR-17, NFR-5)
- Load: 100 RPS GET /v1/transactions with filters and pagination.
- Accept: p95 < 200 ms; success ≥ 99%.

3) Webhook acknowledgment path (FR-7, NFR-4)
- Method: Replay real webhook payloads to /v1/webhooks/authorize-net at 100 RPS.
- Accept: HTTP 200 p95 < 200 ms; metrics show enqueue p95 < 150 ms; 0 schema/auth failures.

4) Webhook end-to-end processing (FR-7, NFR-4)
- Method: Send 1,000 events (mix of success/failure/refund/subscription) with 10% duplicates.
- Accept: p95 processing < 2 s; exactly-once side effects; duplicates suppressed; DLQ size = 0.

5) Idempotency for client requests (FR-6)
- Method: Repeat the same Purchase with identical X-Idempotency-Key N times across multiple clients.
- Accept: One persisted success; all retries return identical response (status/body); idempotency replay count increments.

6) Duplicate and out-of-order webhooks (FR-6, FR-7)
- Method: Replay duplicate events with small reordering.
- Accept: Single side effect per logical event; order state eventually correct; audit log shows dedupe.

7) Concurrency conflict resolution (FR-19)
- Method: Concurrently issue Capture and Refund for the same order (multiple threads).
- Accept: Capture vs refund race resolved deterministically; refund on non-settled returns 409 CONFLICT with CAPTURE_PENDING; data consistent.

8) Rate limiting and abuse controls (NFR-8)
- Method: Exceed per-merchant and public endpoint thresholds.
- Accept: 429 responses with Retry-After; no degradation of other tenants; logs and metrics reflect throttling.

9) Gateway instability and retries (FR-12, NFR-7)
- Method: Inject timeouts/5xx into gateway calls for a subset (e.g., 5%).
- Accept: Exponential backoff applied; bounded retries; error mapping uses GATEWAY_TIMEOUT/GATEWAY_ERROR; p95 remains within budgets for unaffected traffic.

10) Queue durability and recovery (NFR-4, Reliability)
- Method: Stop Worker; send 500 webhooks; restart Worker.
- Accept: All events processed after recovery; ordering best-effort; no loss; poison queue remains empty.

11) Secrets and logging hygiene (Security)
- Method: Grep/scan logs and configs for credentials and PAN/PII.
- Accept: 0 findings; only tokens/last4 retained where applicable.

12) Observability completeness (NFR-1, NFR-2)
- Method: Verify correlation_id on 100% of responses; metrics endpoint includes throughput/latency/error rates, queue depth, idempotency replays.
- Accept: All present and labeled (endpoint, merchant, status); no high-cardinality labels.

## 5. Test execution guidance (local)
- Warm-up: 2–3 minutes at 50% target load before measurement.
- Duration: ≥ 5 minutes per steady-state load; bursts of 60 seconds for spike tests.
- Sampling: Export raw metrics and logs with correlation_id for traceability.
- Isolation: Run API and Worker with consistent resource limits to surface contention.

## 6. Reporting
- Produce TEST_REPORT.md including: summary table (targets vs observed), percentile latencies, error rates, screenshots, and notable metrics graphs.
- Log noteworthy incidents (e.g., throttling onset, DLQ events) with timestamps and correlation IDs.

## 7. Exit criteria
- All acceptance thresholds in Section 3 met.
- No data loss in queue tests; no duplicate side effects under dedupe tests.
- Error responses conform to the standardized schema across all exercised endpoints.
- Logs contain no sensitive data; correlation IDs present throughout.
