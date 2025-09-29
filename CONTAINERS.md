# Container Tracks & One-Command Orchestration

This project is containerised so that `docker-compose up -d` at the repository root brings 
up all workloads. Components are grouped into tracks to mirror system responsibilities.

## Tracks

| Track | Services | Purpose |
|-------|----------|---------|
| `core` | `api`, `db` | Minimal footprint for API development/testing. Includes Authorize.Net HTTP client pointing to sandbox URL. |
| `supporting` | `redis`, `worker` | Queue-backed processing, background jobs. |
| `api` | `api` | Run the HTTP API standalone on-demand. |
| `worker` | `worker` | Run only background worker (requires `db`/`redis`). |

Compose profiles are used to toggle tracks. Combine profiles to tailor stacks.

## Usage

```bash
# Default: run everything
docker-compose up -d

# API + DB only
docker-compose --profile core up -d

# Add worker and redis when testing async flows
docker-compose --profile core --profile supporting up -d

# Tear down
docker-compose down --volumes
```

### Environment

- `SECURITY_JWT_SECRET`: Base64 HS256 secret (defaults to sample value). Override in production-grade runs.
- `AUTHORIZE_NET_LOGIN_ID`, `AUTHORIZE_NET_TRANSACTION_KEY`, `AUTHORIZE_NET_SIGNATURE_KEY`: injected into the API container to configure REST integration. Compose defaults point to sandbox-safe values.
- PostgreSQL credentials default to `payments_user`/`changeit` for local use.

### Images

- `Dockerfile.api`: Builds shaded Spring Boot jar for API service.
- `Dockerfile.worker`: Reuses same jar but runs headless worker entrypoint.

Both use multi-stage builds on JDK 17 and expect Gradle wrapper outputs.

