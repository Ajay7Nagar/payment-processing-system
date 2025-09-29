# Outstanding Tasks

In progress:
- `rem-metrics`: Implement tracing (Brave/OpenTelemetry), correlation propagation, and Micrometer metrics (counters, timers, JVM/system binders) with `/metrics` exposure.
- `authnet-hardening`: Harden Authorize.Net sandbox integration and webhook handling (sandbox credential flow, signature validation, retry resilience).
- `rem-docs`: Restore and update README, API spec, observability docs, compliance guidance.

Pending next:
- ~~`rem-settlement`: Build settlement export service/API and persistence for daily reports.~~ (✅ Implemented)
- `compliance-api`: Expose compliance audit log query endpoints with tenant-aware filtering (single-tenant now).
- `rem-tests`: Extend integration/coverage for new capabilities (metrics, tracing, webhooks, settlement).
- ~~`queue-webhook`: Introduce queue-backed webhook processing for scalability.~~ (✅ Implemented)
