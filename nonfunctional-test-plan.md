# Non-Functional Test Plan

## Objectives
- Validate that the payment processing platform satisfies availability, performance, security, privacy, and observability requirements prior to production release.
- Provide concrete local and pre-production test activities to ensure compliance with SLA, throughput, and data protection expectations.

## Scope
- Applies to API gateway, async workers, persistence tier, and observability stack deployed in dev, staging, and pre-production environments.
- Includes parallel validation using local Docker Compose stack where possible to iterate quickly before staging.

## Environments
- **Local Compose**: Single developer environment with API app, PostgreSQL, RabbitMQ, Prometheus/Grafana, webhook simulator.
- **Staging**: Kubernetes deployment mirroring production configuration within ±5% resource quotas. Authorize.Net sandbox credentials used for live gateway interaction.

## Metrics & Tooling
- **Load Generation**: k6 scripts targeting REST endpoints, tuned to emulate 20–100 RPS depending on scenario.
- **Queue Stress**: Custom job injector publishing to RabbitMQ using seeded payloads to simulate webhook and dunning bursts.
- **Observability Capture**: Prometheus for metrics, Grafana dashboards for visualization, Loki/ELK for structured logs, Jaeger/OTel collector for traces.
- **Availability Checks**: Automated probes using curl or Postman monitors hitting `/actuator/health` and key business endpoints.

## Test Matrix

### 1. Latency & Throughput Tests
- **Objective**: Confirm p95 <300 ms and p99 <500 ms for purchase, refund, and subscription endpoints under 20 concurrent requests.
- **Setup**: k6 running against staging API with live Authorize.Net sandbox; dataset of 500 test customers and vaulted tokens.
- **Procedure**:
  1. Warm up system with 5 minutes at 5 RPS.
  2. Execute stepped load: 10 RPS for 10 minutes, 20 RPS for 10 minutes.
  3. Capture k6 summary, Prometheus latency histogram, JVM metrics.
- **Acceptance Criteria**:
  - p95 latency ≤300 ms and p99 ≤500 ms excluding external gateway latency (validated via Authorize.Net mock timings).
  - Error rate (HTTP 5xx) <0.5% and no saturation of thread pools or DB connections.
- **Local Check**: Run k6 in local Compose for smoke verification (lower RPS) to catch obvious regressions.

### 2. Availability & Resilience Tests
- **Objective**: Demonstrate ≥99.9% availability over rolling 30-day cycle and verify automatic recovery from component failures.
- **Setup**: Staging environment with Kubernetes liveness/readiness probes enabled.
- **Procedure**:
  1. Run synthetic availability monitor (Postman/Locust) hitting purchase endpoint every 1 minute over 72 hours.
  2. Simulate worker pod crash and database failover window using chaos scripts; observe recovery time.
- **Acceptance Criteria**:
  - Availability ≥99.9% during test window.
  - Failed pods restart within 60 seconds; queue backlog drained within 2 minutes after worker recovery.

### 3. Queue Backlog & Dunning Burst
- **Objective**: Ensure async worker drains 6,000 messages/hour bursts (100 events per minute) within 2 minutes after burst subsides.
- **Setup**: Local Compose with RabbitMQ, or staging with sandbox payloads.
- **Procedure**:
  1. Inject 600 webhook events over 6 minutes using simulator.
  2. Monitor queue depth metrics, worker logs, and DLQ counts.
- **Acceptance Criteria**:
  - Queue depth returns to baseline within 120 seconds of injection stop.
  - DLQ count <1% and no lost acknowledgements.

### 4. Idempotency Stress Test
- **Objective**: Validate idempotent handling of duplicate purchase, capture, and webhook events.
- **Setup**: Local Compose and staging.
- **Procedure**:
  1. Fire 100 duplicate purchase requests per merchant with identical `Idempotency-Key` using k6.
  2. Replay webhook payloads 5 times each via simulator.
- **Acceptance Criteria**:
  - Duplicate requests return consistent response payloads without additional DB state changes.
  - Audit logs show single entry per unique operation; duplicates logged as ignored with reference to original.

### 5. Security & Access Control
- **Objective**: Ensure JWT enforcement, rate limiting, and audit logging of authentication failures.
- **Setup**: Local Compose (with mocked JWT key store) and staging.
- **Procedure**:
  1. Attempt API access with expired, tampered, and missing JWTs.
  2. Exceed configured rate limits using curl loops.
- **Acceptance Criteria**:
  - Responses return 401 or 429 with proper error model.
  - Audit logs capture each failure with merchant context.

### 6. Data Privacy & GDPR Checks
- **Objective**: Validate soft-delete/anonymization workflow within 24 hours.
- **Setup**: Staging environment with realistic data masking.
- **Procedure**:
  1. Trigger PII deletion for sample customer.
  2. Run scheduled anonymization job; query database and logs for residual PII.
- **Acceptance Criteria**:
  - Customer record retains transaction identifiers while PII columns anonymized within 24 hours.
  - Logs/traces redact PII values.

### 7. Audit & Compliance API Verification
- **Objective**: Confirm retrieval of immutable audit logs with filtering and retention.
- **Setup**: Staging compliance role credentials.
- **Procedure**:
  1. Insert operations (purchase, refund, subscription change) and corresponding audit entries.
  2. Query `/api/v1/audit/logs` filtering by date range, operation, and actor role.
- **Acceptance Criteria**:
  - API returns filtered results within 2 seconds for 10k records.
  - Attempted modification of logs rejected with 403 and audit trail entry.

### 8. Settlement Export Performance
- **Objective**: Validate daily export generation time and accessibility.
- **Setup**: Staging with at least 5,000 settled transactions across merchants.
- **Procedure**:
  1. Trigger settlement export via API; monitor async job completion.
  2. Measure generation time and inspect output artifacts.
- **Acceptance Criteria**:
  - Export completes within 5 minutes for stated volume.
  - Generated file available via signed URL/API with correct settlement batch IDs.

### 9. Observability Coverage
- **Objective**: Ensure metrics, logs, and traces provide coverage for incident response.
- **Setup**: Local Compose and staging.
- **Procedure**:
  1. Validate `/actuator/metrics` exposes key counters (latency, queue depth, error rate).
  2. Inspect Grafana dashboards during load tests; confirm correlation IDs present in logs.
  3. Trace sample purchase through API and worker spans.
- **Acceptance Criteria**:
  - Metrics refresh at least every 60 seconds.
  - Logs include merchant ID and correlation ID.
  - Traces show Authorize.Net call spans with timing annotations.

### 10. Regression Guardrails in CI
- **Objective**: Maintain ≥80% automated test coverage and catch performance regressions early.
- **Setup**: CI pipeline executing Gradle/Maven test tasks and k6 smoke tests.
- **Procedure**:
  1. Run unit/integration tests with coverage reports.
  2. Execute short k6 smoke (2 minutes at 5 RPS) against ephemeral environment.
- **Acceptance Criteria**:
  - Coverage ≥80%; failure blocks merge.
  - Smoke test latency within 20% of baseline; deviations trigger alert.

## Reporting
- Aggregate metrics, logs, and test artifacts stored in centralized repository (e.g., S3 bucket or test results DB).
- Non-functional test summary generated per release candidate including pass/fail status, deviations, and remediation actions.

## Exit Criteria
- All acceptance criteria satisfied in staging within 14 days preceding production cutover.
- No unresolved Sev-1/Sev-2 defects related to non-functional requirements.
- Observability dashboards reviewed and signed off by engineering and SRE leads.

