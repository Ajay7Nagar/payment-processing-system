# Testing Strategy

## Goals
- Ensure the payment processing platform meets functional and non-functional requirements defined in `raw-requirement.txt` and `requirements.md`.
- Provide confidence in payment flow correctness, tenant isolation, resilience, security, and compliance readiness before production deployment.
- Align testing activities with modular monolith architecture (API gateway, domain modules, async workers, persistence, observability).

## Testing Pillars
- **Functional Validation**: Confirm core and advanced payment flows operate correctly against Authorize.Net sandbox.
- **Quality Attributes**: Verify performance, availability, scalability, security, privacy, and observability.
- **Automation First**: Maximize automated verification (unit, integration, contract, performance) with ≥80% coverage and continuous feedback.
- **Shift Left**: Run fast feedback tests locally via Docker Compose; promote to staging for full-system validation.

## Test Types & Coverage

### 1. Unit Tests
- **Scope**: Domain services, validators, mappers, utility classes across `payments`, `billing`, `reporting`, `webhook`, and shared modules.
- **Key Scenarios**:
  - Payment state machine transitions (authorize → capture → settle, cancel, refund).
  - Subscription schedule calculations, dunning sequence generation.
  - Idempotency key collision handling and timeline event generation.
  - Error mapping to structured error responses.
- **Tooling**: JUnit 5 + Mockito (or equivalent) with coverage reporting in CI.
- **Target**: ≥80% line and branch coverage; high priority on business-critical logic.

### 2. Component Tests
- **Scope**: REST controllers, service facades, repositories using Spring Boot test slices with Testcontainers (PostgreSQL, RabbitMQ).
- **Key Scenarios**:
  - Purchase/authorize endpoints persisting transactions with correlation IDs.
  - Conflict detection on duplicate idempotency keys.
  - Single-tenant repository lookups using globally scoped IDs.
  - Subscription creation updating billing schedules and proration credits.
  - Webhook dedup persistence, queue publication, and retry scheduler behavior (with RabbitMQ Testcontainers).
- **Goal**: Validate wiring and persistence behavior without external dependencies.

### 3. Integration Tests
- **Scope**: End-to-end flows against Authorize.Net sandbox using real HTTP calls.
- **Key Scenarios**:
  - Purchase flow and refund confirmations verifying gateway responses.
  - Two-step authorize/capture with error retry simulation.
  - Webhook ingestion verifying signature validation, queue enqueue, and worker consumption.
- **Execution**: Run in staging and dedicated integration pipeline due to external dependency latency.

### 4. Contract Tests
- **Scope**: Provider contracts for API responses, consumer contracts for outbound Authorize.Net interactions.
- **Approach**: Use OpenAPI-driven tests or Pact contracts to ensure backward compatibility and maintain stable client integrations.
- **Use Cases**: Validate response schemas for payments/subscriptions endpoints; verify Authorize.Net request payload formats.

### 5. Performance & Load Tests
- **Scope**: Validate latency, throughput, and resource utilization under expected peak loads.
- **Scenarios**:
  - Purchase, refund, subscription endpoints at 20 RPS (steady) and 100 RPS bursts (stress).
  - Worker throughput for 100 webhook events/minute and dunning bursts.
- **Tooling**: k6 scripts, Prometheus/Grafana for metric capture.
- **Targets**: p95 <300 ms, p99 <500 ms; queue backlog drained within 2 minutes; CPU/RAM within budget.

### 6. Security Tests
- **Scope**: JWT validation, rate limiting, data access controls, secrets handling.
- **Activities**:
  - Automated checks for token expiration, signature tampering, and missing claims.
  - Static analysis (SAST) and dependency scanning (OWASP Dependency-Check).
  - Penetration-style scripts for injection, broken access control, SSRF entry points.
- **Outputs**: Security test report, remediation plan for findings.

### 7. Privacy & Compliance Tests
- **Scope**: GDPR anonymization, PCI DSS log redaction, audit log immutability, data retention policies.
- **Approach**:
  - Automated tests verifying anonymization job behavior.
  - Manual checks for log redaction and retention configuration.
  - Compliance API functional tests evaluating filtering accuracy and access controls.

### 8. Observability Verification
- **Scope**: Ensure metrics, logs, and traces meet NFR-7 requirements.
- **Checks**:
  - Automated smoke test verifying `/metrics`, `/actuator/health`, and tracing headers on key endpoints.
  - Log parsing tests confirming correlation IDs and merchant IDs present without PII.
  - Tracing tests validating spans cover API → domain → external gateway → worker flows.

### 9. Chaos & Resilience Tests
- **Scope**: Validate behavior under component failures (gateway outage, queue downtime, DB failover).
- **Execution**: Chaos tooling (Litmus or custom scripts) in staging.
- **Scenarios**:
  - Simulate Authorize.Net timeout and observe circuit breaker behavior.
  - Kill worker pod during backlog processing; ensure DLQ usage and retry policies.
- **Success Criteria**: Graceful degradation messages, automatic recovery, no data loss.

### 10. User Acceptance & Scenario Tests
- **Participants**: Product owners, QA, compliance officers.
- **Focus**: Validate business scenarios end-to-end (recurring billing lifecycle, dispute flows, compliance queries).
- **Artifacts**: Test scripts derived from acceptance tests (AT-1 to AT-10).

## Test Environments & Data
- **Local**: Docker Compose stack for rapid iteration with seeded merchant/customer data.
- **CI**: Ephemeral environments spun via containers for unit/component/contract tests.
- **Staging**: Mirrors production with masked data, integrates with Authorize.Net sandbox and S3-compatible storage.
- **Data Management**: Synthetic data sets with realistic volumes; anonymized snapshots for staging.

## Automation & Toolchain
- **Build/Test Runner**: Gradle or Maven with profiles for unit, component, integration suites.
- **CI/CD Integration**: GitHub Actions (or equivalent) executing tiered pipelines (unit → component → integration → performance smoke → deployment).
- **Static Analysis**: SpotBugs, Checkstyle, SonarQube integration for maintainability and security hotspots.
- **Secrets Scanning**: Trufflehog or GitHub secret scanning enforced on commits.

## Test Data Strategy
- **Deterministic Fixtures**: Reusable datasets per test type with isolated merchant IDs.
- **Randomized Inputs**: Use property-based testing for edge cases in domain logic (amount rounding, schedule calculations).
- **Gateway Simulation**: Mock servers for Authorize.Net during unit/component tests; sandbox for integration.

## Reporting & Governance
- **Dashboards**: CI pipeline status, coverage trends, performance baselines in Grafana.
- **Artefacts**: Store test reports (JUnit XML, coverage HTML, k6 summaries) in centralized storage per run.
- **Traceability**: Map tests to requirements (FR/NFR) in tracking document for auditability.

## Exit Criteria for Release
- All critical and high-severity defects resolved.
- All acceptance tests (AT-1 to AT-10) pass in staging.
- Non-functional benchmarks meet SLA/SLO targets (latency, availability, throughput).
- Security scans show no unresolved high/critical vulnerabilities; compliance checks signed off.
- Observability dashboards reviewed and approved by engineering + SRE.

## Continuous Improvement
- Post-release testing review capturing incidents, gaps, and new scenarios.
- Update testing strategy quarterly or on major architectural changes (e.g., extracting modules, scaling new tenants).
- Extend automation coverage as new features (e.g., additional payment methods) are introduced.

