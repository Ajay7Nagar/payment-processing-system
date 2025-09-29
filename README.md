# payment-processing-system

API spec: see `API-SPECIFICATION.yml`.

Key endpoints updated:
- POST `/v1/payments/purchase` → 201 with `Payment` body
- POST `/v1/payments/cancel` → 200 with `CancelResponse` body

Headers:
- `X-Correlation-Id` on responses
- `X-API-Version: v1` on responses