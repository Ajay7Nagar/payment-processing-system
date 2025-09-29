# Compliance Guidance

## Goals
- Demonstrate readiness for GDPR, PCI DSS SAQ-A, and SOC2 Type II controls.
- Maintain immutable audit logs with fine-grained filtering and export capabilities for compliance officers.
- Ensure security and privacy requirements (FR-13, FR-17, NFR-4, NFR-5, NFR-9) are met with actionable procedures.

## Audit Logging
- All state-changing operations (payments, refunds, subscription updates, settlements) invoke `PaymentAuditService` which records `AuditLog` entries.
- Audit logs capture: `actor`, `operation`, `resourceType`, `resourceId`, `metadata` (JSON payload), `ipAddress`, and timestamps.
- `AuditLog` entries are append-only (no updates/deletes). Database constraints enforce immutability; logical deletions require compensating entries and approval.
- IP addresses and correlation IDs allow incident response teams to trace user actions without storing sensitive PII.

## Compliance API
- Endpoints:
  - `GET /api/v1/compliance/audit-logs`: paginated query with filters (date range, actor, operation, resource type/ID).
  - `POST /api/v1/compliance/audit-logs/export`: returns filtered logs for offline review (JSON payload).
- Access restricted via Spring Security role `ROLE_COMPLIANCE_OFFICER`. JWT roles verified by `SecurityConfiguration`.
- Input validation ensures malicious filters (e.g., invalid UUIDs) return 400 with structured errors.
- Rate limiting recommended via API gateway (out of scope in codebase) to prevent abuse.

## Data Retention & Privacy
- Audit logs retained for ≥7 years per FR-13 / NFR-5. Flyway migrations provision indexes (`V6__compliance_indexes.sql`) for efficient historical queries.
- Customer PII anonymized via scheduled jobs (future work) but audit metadata stores hashed references where feasible.
- GDPR erasure requests: remove PII from business tables while preserving audit records with anonymized metadata; document chain-of-custody in compliance runbook.

## Operational Procedures
- Compliance officers authenticate with RS256 JWTs issued by identity provider; ensure tokens include `roles: [COMPLIANCE_OFFICER]`.
- Audit exports should be stored in secure, access-controlled locations (e.g., encrypted S3 bucket). Future enhancement: streaming CSV/Parquet exports.
- Monitor `audit_logs` table growth; consider partitioning by month/year for archival in long-term storage.
- Incident response: use correlation IDs and audit entries to reconstruct timeline; attach audit export to incident ticketing system.

## Testing & Validation
- `ComplianceAuditServiceTest` verifies filtering logic, ensuring optional parameters produce correct repository specs.
- `ComplianceAuditControllerTest` enforces RBAC, validates 401/403 responses, and ensures JSON contract.
- Manual checklist (staging):
  1. Generate payment/refund activity, confirm audit entries exist.
  2. Hit compliance API with date filters; verify only expected operations appear.
  3. Attempt API access with missing/invalid JWT; expect 401.
  4. Export logs and confirm metadata contains actor/resource context without PII exposure.

## Future Enhancements
- Add CSV export option with columnar formatting for standard audit tools.
- Integrate with SIEM pipeline to push audit events to centralized monitoring (e.g., Splunk, ELK).
- Automate compliance dashboards tracking API usage, export history, and alert on suspicious activity patterns.
