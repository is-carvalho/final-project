# Quickstart: E-Commerce Order Management

## Prerequisites

- Java 21
- Docker and Docker Compose
- Maven Wrapper from this repository

## Local Infrastructure

1. Add Docker Compose services for PostgreSQL, WireMock, Prometheus, Grafana, Jaeger, and optional Keycloak/JWK support.
2. Place external service mappings in `wiremock/mappings/`.
3. Start dependencies:

```powershell
docker compose up -d postgres wiremock prometheus grafana jaeger
```

## Run the Service

```powershell
.\mvnw spring-boot:run
```

Expected outcome: the order service starts, connects to PostgreSQL, runs Flyway migrations, exposes API endpoints under `/api/v1`, and publishes actuator/metrics endpoints for Prometheus.

## Validation Scenarios

### Create and Edit Draft Order

- Send `POST /api/v1/orders` with a valid customer, `Authorization: Bearer <token>`, `Correlation-Id`, and `Idempotency-Key`.
- Add a valid product through `POST /api/v1/orders/{id}/items`.
- Add the same product again.
- Expected outcome: one order item exists for the product and its quantity is increased.

### Confirm and Lock Order

- Send `POST /api/v1/orders/{id}/confirm`.
- Try to add or remove an item afterward.
- Expected outcome: confirmation recalculates current prices and later item changes return Problem Details errors.

### Cancel Before Payment Approval

- Cancel a draft, confirmed, pending, or rejected order with `DELETE /api/v1/orders/{id}`.
- Try to cancel a payment-approved order.
- Expected outcome: cancellation succeeds before approval and fails after approval.

### Payment Idempotency and Callback Replay

- Send `POST /api/v1/payments` for a confirmed order with an `Idempotency-Key`.
- Repeat the same request with the same key.
- Send `POST /api/v1/payments/{id}/callback` twice with the same provider event id.
- Expected outcome: no duplicate payment transaction is created and callback replay has no extra side effects.

### Retry Limit

- Configure WireMock payment mappings to return rejection.
- Initiate/retry payment until the third rejection.
- Expected outcome: the third rejection automatically cancels the order and later payment attempts are rejected.

## Test Commands

```powershell
.\mvnw test
.\mvnw verify
.\mvnw org.pitest:pitest-maven:mutationCoverage
```

Expected outcomes: domain coverage is at least 80%, Pitest mutation score is at least 75% for the domain module/package, integration tests start PostgreSQL and WireMock through Testcontainers, and API contract tests remain aligned with `specs/001-order-management-service/contracts/openapi.yaml`.
