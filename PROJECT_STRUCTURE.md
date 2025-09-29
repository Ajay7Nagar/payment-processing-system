# Project Structure

## Overview
The project follows the **Layered + Bounded Context Roots** layout (Alternative B), organizing code by adapters, application orchestration, domain contexts, infrastructure, and worker runtime. This structure supports the modular monolith architecture while keeping REST APIs, messaging endpoints, and persistence adapters aligned with domain-driven boundaries.

## Layout
```
/src/main/java/com/example/payments
├── adapters
│   ├── api
│   ├── messaging
│   └── persistence
├── application
│   ├── orchestration
│   ├── scheduling
│   └── services
├── domain
│   ├── billing
│   ├── payments
│   ├── reporting
│   └── shared
├── infra
└── worker
```

### `adapters`
- `api`: Hosts REST controllers, request/response mapping, and HTTP exception translation aligned with the API gateway responsibilities.
- `messaging`: Contains RabbitMQ publishers, consumers, and message converters bridging async workers with the domain layer.
- `persistence`: Provides Spring Data implementations, entity mapping configuration, and transactional adapters for the shared persistence layer.

### `application`
- `orchestration`: Coordinates multi-step use cases across bounded contexts, enforcing idempotency and transaction boundaries.
- `scheduling`: Defines cron triggers, queue enqueue logic, and job orchestration for recurring billing and settlements.
- `services`: Exposes application-level facades invoked by adapters to execute domain workflows.

### `domain`
- `billing`: Encapsulates subscription lifecycle rules, dunning policies, and billing aggregates.
- `payments`: Models payment orders, transactions, and state transitions for purchase/refund flows.
- `reporting`: Manages settlement projections, export preparation, and audit-oriented domain logic.
- `shared`: Holds reusable value objects, domain events, and exception types that span contexts.

### `infra`
- Centralizes Spring configuration, security setup, observability utilities, feature toggles, and cross-cutting platform services shared across modules.

### `worker`
- Provides the asynchronous worker entry point, queue listener wiring, and retry management aligned with the async worker module.

