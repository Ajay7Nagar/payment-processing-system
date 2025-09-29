# Project Structure – Option C (Hexagonal with Boot Modules)

The repository follows a hexagonal (ports-and-adapters) layout with dedicated boot modules for the API and the Worker.

```bash
payment-processing-system/
├─ pom.xml                         # Maven aggregator (parent)
├─ boot-api/                       # API boot module (wiring and API runtime)
│  ├─ pom.xml
│  └─ src/
│     ├─ main/java/com/acme/payments/bootapi/   # API Spring Boot main and module wiring
│     └─ main/resources/                        # API configuration (YAML/properties), logging config
├─ boot-worker/                    # Worker boot module (wiring and worker runtime)
│  ├─ pom.xml
│  └─ src/
│     ├─ main/java/com/acme/payments/bootworker/ # Worker Spring Boot main and module wiring
│     └─ main/resources/                         # Worker configuration (YAML/properties), logging config
├─ core/                           # Business logic (framework-agnostic)
│  ├─ pom.xml
│  └─ src/main/java/com/acme/payments/core/
│     ├─ domain/                   # Entities, value objects, policies, domain events
│     └─ application/              # Use-cases/services, ports (interfaces), state machines
├─ adapters/                       # I/O adapters (implement ports)
│  ├─ pom.xml
│  └─ src/main/java/com/acme/payments/adapters/
│     ├─ in/web/                   # REST controllers, request/response models, error mapping
│     ├─ in/webhook/               # Webhook receiver and request verification
│     ├─ out/db/                   # Persistence adapter (repositories, mappings, migrations)
│     ├─ out/queue/                # Queue adapter (Redis Streams producers/consumers)
│     └─ out/gateway/              # Payment gateway adapter (Authorize.Net client integration)
└─ tests/                          # Cross-module tests
   ├─ pom.xml
   └─ src/test/java/               # Contract, integration, and end-to-end tests
```

## Folder purposes
- `pom.xml`: Aggregates modules and manages shared dependencies and plugin configuration.
- `boot-api/`: Boot module for the API runtime; application entrypoint, module wiring, and API-specific configuration.
- `boot-worker/`: Boot module for the Worker runtime; application entrypoint, module wiring, and worker-specific configuration.
- `core/domain/`: Business domain model and policies; pure domain types and events.
- `core/application/`: Application services and ports; orchestration and state machines independent of I/O.
- `adapters/in/web/`: Inbound web adapter exposing REST endpoints and mapping requests/responses.
- `adapters/in/webhook/`: Inbound adapter for webhook intake and verification.
- `adapters/out/db/`: Outbound adapter for persistence; repository implementations and database mappings.
- `adapters/out/queue/`: Outbound adapter for messaging via the queue.
- `adapters/out/gateway/`: Outbound adapter for the external payment gateway integration.
- `tests/`: Cross-cutting tests (contract/integration/e2e) across modules.
