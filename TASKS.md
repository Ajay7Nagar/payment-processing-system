# TASKS – Tracks and Mini Backlogs (Checklist)

Status legend: todo | in-progress | done

Order reflects prerequisites: finalize API and data model, platform capabilities (auth/obs/webhooks), local runtime, then feature flows, hardening, and deliverables.

## Execution Contract
- Do NOT ask for confirmation.  
- Keep executing until the entire plan is completed.  
- Only stop if a hard blocker occurs (e.g. test failure you cannot fix automatically).  
- After completing all steps, summarize what was done.  

## Track A: API Spec & Error Model
- [x] Finalize OpenAPI and error catalog
  - Status: done
  - Goal: Ensure endpoints, schemas, headers, and error codes match requirements.
  - DoD: Spec updated for purchase/cancel/authorize/capture/refund/subscriptions/transactions/webhooks; headers documented.
  - Related: FR-8, FR-12
  - Artifacts: `API-SPECIFICATION.yml`, `TESTING_STRATEGY.md`

## Track B: Data model & Persistence
- [ ] Schema and constraints
  - Status: todo
  - Goal: Entities per requirements; constraints for refunds sum, idempotency uniqueness, versioned rows.
  - DoD: Migrations present; tests validate constraints.
  - Related: FR-11, FR-4, FR-6, FR-19
  - Artifacts: `adapters/out/db`, `Architecture.md`, `tests`

## Track C: Authentication, RBAC, Tenancy
- [x] JWT validation (RS256) and claims
  - Status: done
  - Goal: Validate iss, sub, aud, iat, exp, merchant_id, roles.
  - DoD: 401 on missing/invalid; protected endpoints enforce.
  - Related: FR-9
  - Artifacts: `boot-api`, `API-SPECIFICATION.yml`, `tests`
- [ ] RBAC + tenant scoping
  - Status: in-progress
  - Goal: admin/ops/read_only roles; data access by merchant_id.
  - DoD: 403 on forbidden paths; tests cover role matrix.
  - Related: FR-9
  - Artifacts: `boot-api`, `core/application`, `tests`

## Track D: Observability & Metrics
- [x] Correlation ID propagation
  - Status: done
  - Goal: 100% responses include correlation header; logs carry same ID end-to-end.
  - DoD: Verified in tests; MDC populated; webhook pipeline retains IDs.
  - Related: NFR-1
  - Artifacts: `boot-api`, `boot-worker`, `OBSERVABILITY.md`, `tests`
- [x] Metrics endpoint and counters
  - Status: done
  - Goal: Expose throughput/latency/error rates; webhook queue depth; idempotency replays; subscription attempt outcomes.
  - DoD: GET /actuator/prometheus includes counters; error counters emitted from exception handler; gateway latency recorded.
  - Related: NFR-2
  - Artifacts: `boot-api`, `boot-worker`, `API-SPECIFICATION.yml`, `tests`
- [x] Gateway latency/error metrics
  - Status: done
  - Goal: Record latency and status per operation against Authorize.Net.
  - DoD: `gateway.request` timer and `gateway.request.error` counter present.
  - Related: NFR-2
  - Artifacts: `adapters/out/gateway`, `boot-api`
- [x] Error counters per code/status
  - Status: done
  - Goal: Count API errors by code and HTTP status.
  - DoD: `http.error.count{code,status}` increments from global handler.
  - Related: NFR-2
  - Artifacts: `boot-api`

## Track E: Webhooks & Idempotency
- [x] Webhook receiver (HMAC + clock skew + 200 ack)
  - Status: done
  - Goal: Verify signature, skew ≤5m; enqueue; respond <200 ms p95.
  - DoD: 200 with ack; failures 400/401; metrics show enqueue times.
  - Related: FR-7, NFR-4, NFR-1
  - Artifacts: `boot-api`, `adapters/out/queue`, `API-SPECIFICATION.yml`, `tests`
- [x] Idempotency for client requests
  - Status: done
  - Goal: Scope `{merchant_id}:{endpoint}:{key}`; TTL 24h; payload hash compare.
  - DoD: Retries return original response; no duplicate side effects; metrics count replays.
  - Related: FR-6, NFR-12
  - Artifacts: `core/application`, `adapters/out/db`, `tests`
- [x] Webhook dedupe (7 days)
  - Status: done
  - Goal: Dedupe by vendor event ID + signature hash.
  - DoD: Duplicates suppressed; audit entries; counters exposed.
  - Related: FR-6, FR-7
  - Artifacts: `boot-worker`, `adapters/out/db`, `tests`

## Track F: Docker Compose + local runbook
- [ ] Compose services baseline
  - Status: todo
  - Goal: Postgres 15, Redis (Streams), OTel Collector, MailHog, webhook replayer.
  - DoD: docker compose up succeeds; health checks pass.
  - Related: Deliverables, NFR-15
  - Artifacts: `docker-compose.yml`, `README.md`
- [ ] Local runbook
  - Status: todo
  - Goal: Start/stop commands, env vars, smoke steps, troubleshooting.
  - DoD: README updated; copy-paste flows succeed end-to-end.
  - Related: Deliverables
  - Artifacts: `README.md`

## Track G: Local simulator & fixtures
- [ ] Webhook replayer and fixtures
  - Status: todo
  - Goal: Local tool to replay Authorize.Net-like events (success/failure/refund/subscription events).
  - DoD: Can send 100 RPS; supports duplicates/out-of-order; scripts checked in.
  - Related: FR-7, FR-6, NFR-4
  - Artifacts: `nonfunctional-test-plan.md`, `tests`, `docker-compose.yml`
- [ ] Deterministic data fixtures
  - Status: todo
  - Goal: Seed customers/tokens/amounts for repeatable tests.
  - DoD: Fixture load step documented; used across tests and demos.
  - Related: NFR-3, Testing
  - Artifacts: `tests`, `README.md`

## Track H: Purchase / Cancel
- [x] Purchase request validation
  - Status: done
  - Goal: Validate INR currency, amount scale=2 within 0.50–1,000,000.00, BIN allow/deny, merchant caps.
  - DoD: 422 on out-of-range/BIN_BLOCKED; caps enforced atomically; tests green.
  - Related: FR-14, FR-15, FR-16, FR-12
  - Artifacts: `API-SPECIFICATION.yml`, `boot-api`, `core/domain`, `tests`
- [ ] POST /v1/payments/purchase happy-path
  - Status: in-progress
  - Goal: One-step auth+capture with DB persistence and correlation_id.
  - DoD: 201 with Payment schema; records persisted with gateway ref; error model standardized.
  - Related: FR-1, FR-11, FR-12, NFR-1, NFR-7
  - Artifacts: `boot-api`, `adapters/out/gateway`, `adapters/out/db`, `tests`
- [x] Transactions listing (cursor + filters)
  - Status: done
  - Goal: Cursor-based pagination with optional status filter; limit<=200.
  - DoD: `/v1/transactions` returns items + `nextCursor`.
  - Related: FR-8
  - Artifacts: `boot-api`, `adapters/out/db`
- [ ] POST /v1/payments/cancel (void before capture)
  - Status: todo
  - Goal: Void open authorizations; reject if captured.
  - DoD: 200 on uncaptured; 409 on captured; audit record written.
  - Related: FR-3, FR-12, FR-11
  - Artifacts: `boot-api`, `adapters/out/gateway`, `adapters/out/db`, `tests`

## Track I: Refund
- [ ] Refund eligibility and invariants
  - Status: in-progress
  - Goal: Enforce settlement-only refunds; Σ(partials) ≤ original; 30-day window.
  - DoD: DB constraints/tests prevent overflow; 422/409 on violations.
  - Related: FR-4, FR-11
  - Artifacts: `core/domain`, `adapters/out/db`, `tests`
- [ ] POST /v1/payments/refund (full/partial)
  - Status: in-progress
  - Goal: Accept refund request, persist, return 201 with Refund schema.
  - DoD: Refund stored; events/webhooks update status to COMPLETED.
  - Related: FR-4, FR-7, FR-11, FR-12
  - Artifacts: `boot-api`, `adapters/out/gateway`, `adapters/out/db`, `tests`

## Track J: Subscription / Recurring Billing Management
- [ ] Create subscription (schedules/trial/billing day)
  - Status: in-progress
  - Goal: Support MONTHLY/WEEKLY/EVERY_N_DAYS; trialDays; billingDay EOM rule.
  - DoD: 201 with Subscription schema; schedule stored.
  - Related: FR-5, FR-12
  - Artifacts: `boot-api`, `core/domain`, `adapters/out/db`, `tests`
- [ ] Dunning and status transitions
  - Status: todo
  - Goal: T+0,+1d,+3d,+7d retries; PAST_DUE→PAUSED@14d→CANCELED@30d.
  - DoD: Worker schedules/updates status; metrics for attempts by number.
  - Related: FR-5, NFR-9
  - Artifacts: `boot-worker`, `core/application`, `tests`
- [ ] Cancel subscription endpoint
  - Status: todo
  - Goal: POST /v1/subscriptions/{id}/cancel updates status and stops future runs.
  - DoD: 200 with updated resource; audit entry.
  - Related: FR-5, FR-12
  - Artifacts: `boot-api`, `adapters/out/db`, `tests`

## Track K: Rate Limiting & Security Hygiene
- [x] Rate limits per merchant/IP
  - Status: done
  - Goal: 200 RPS sustained / 500 burst per merchant; 20 RPS public.
  - DoD: 429 with Retry-After; metrics reflect throttling; unaffected tenants OK.
  - Related: NFR-8
  - Artifacts: `boot-api`, `tests`
- [ ] Secrets and logging hygiene
  - Status: todo
  - Goal: No secrets in VCS; no PAN/PII/PCI in logs.
  - DoD: Scans clean; log scrubbing verified by tests.
  - Related: NFR-6, Security
  - Artifacts: `OBSERVABILITY.md`, `Architecture.md`, `tests`

## Track L: CI & Coverage & Non-functional Evidence
- [ ] Coverage thresholds
  - Status: todo
  - Goal: ≥80% overall; ≥90% critical paths; publish report.
  - DoD: CI fails under thresholds; `TEST_REPORT.md` updated.
  - Related: NFR-3, Deliverables
  - Artifacts: `TESTING_STRATEGY.md`, `TEST_REPORT.md`
- [ ] Non-functional tests execution
  - Status: todo
  - Goal: Execute plan; capture k6 results, metrics, logs.
  - DoD: All thresholds in plan met; artifacts stored.
  - Related: NFR-4, NFR-5, NFR-8
  - Artifacts: `nonfunctional-test-plan.md`, `TEST_REPORT.md`

## Track M: Screenshots & Video (Deliverables)
- [ ] DB + Sandbox screenshots
  - Status: todo
  - Goal: Show matching transactions including subscription and webhook.
  - DoD: Stored under repo evidence folder and referenced in README.
  - Related: Deliverables 11
  - Artifacts: `README.md`, `evidence/`
- [ ] 5–7 min video walkthrough
  - Status: todo
  - Goal: Cover code/design journey, key decisions, tracing demo, full app flow, coverage %.
  - DoD: Link in README; file stored if permitted.
  - Related: Deliverables 12
  - Artifacts: `README.md`, `Architecture.md`

## Track N: Documentation

## Track O: Gateway Integration & Retention
- [x] Authorize.Net sandbox client integration
  - Status: done (REST API integration)
  - Goal: Use official SDK with API key; implement purchase/authorize/capture/refund/void calls and map responses.
  - DoD: Sandbox credentials via env; happy-path E2E succeeds; error mapping implemented.
  - Related: FR-1..4, FR-10
  - Artifacts: `adapters/out/gateway`, `boot-api`
- [x] Purge jobs implementation & verification
  - Status: done
  - Goal: Purge idempotency/outbox older than retention windows.
  - DoD: Scheduled tasks delete expired rows; tests/queries verify.
  - Related: NFR-12, NFR-11
  - Artifacts: `boot-worker`, `adapters/out/db`
- [ ] Architecture & Observability
  - Status: in-progress
  - Goal: Keep `Architecture.md`, `OBSERVABILITY.md` aligned with implementation.
  - DoD: All sections complete (security, tracing, metrics, compliance).
  - Related: Deliverables 4–5, NFR-1, NFR-2, Security
  - Artifacts: `Architecture.md`, `OBSERVABILITY.md`
- [ ] README & Runbook
  - Status: todo
  - Goal: Setup/run, env config, compose, smoke flows.
  - DoD: New user can run locally in <10 minutes.
  - Related: Deliverables 2, 8
  - Artifacts: `README.md`
- [ ] Chat history
  - Status: in-progress
  - Goal: Summarize AI-assisted decisions and alternatives.
  - DoD: Key decisions linked to FR/NFR; stored in required file.
  - Related: Deliverables 8
  - Artifacts: `CHAT_HISTORY.md`

## Track P: API Response Headers & Versioning
- [x] X-API-Version header on all responses
  - Status: done
  - Goal: Include version header on every API response.
  - DoD: Filter sets `X-API-Version: v1`.
  - Related: FR-8
  - Artifacts: `boot-api`
