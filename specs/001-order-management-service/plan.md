# Implementation Plan: E-Commerce Order Management

**Branch**: `001-order-management-service` | **Date**: 2026-06-16 | **Spec**: `specs/001-order-management-service/spec.md`

**Input**: Feature specification from `specs/001-order-management-service/spec.md` plus project constraints from `for_plain.txt` and `.specify/memory/constitution.md`.

## Summary

Implement only the `order-service` for the e-commerce order lifecycle: draft creation, item management, confirmation, cancellation, payment initiation, payment retry control with a maximum of 3 rejected attempts, automatic cancellation after the third rejection, and idempotent payment result handling. The service will be a Java 21 / Spring Boot 4.x web service using Clean Architecture, PostgreSQL persistence, Flyway migrations, JWT scope-based authorization, RFC 7807 error responses, structured JSON logs, metrics, traces, and real HTTP calls to external customer, catalog, payment, and notification contexts represented by WireMock standalone in local and test environments.

## Technical Context

**Language/Version**: Java 21 with Spring Boot 4.1.0 already configured in `pom.xml`.

**Primary Dependencies**: Spring Web MVC, Spring Data JPA, Spring Security OAuth2 Resource Server, Flyway, PostgreSQL JDBC driver, Spring WebClient, Resilience4j, Micrometer, OpenTelemetry, Lombok where useful, Problem Details support through Spring MVC exception handling.

**Storage**: PostgreSQL for transactional order, item, payment, payment event, and idempotency records. Flyway owns versioned schema migrations.

**Testing**: JUnit 5, Spring Boot test slices, Testcontainers for PostgreSQL and WireMock, Pitest for domain mutation testing, contract/API tests against `specs/001-order-management-service/contracts/openapi.yaml`.

**Target Platform**: Containerized Linux web service run locally through Docker Compose and in CI through GitHub Actions.

**Project Type**: Single backend web service.

**Performance Goals**: 95% of order lifecycle operations complete within 2 seconds under expected development/test workload. External calls use bounded timeouts and resilience policies.

**Constraints**: Only `order-service` production code is implemented. Customer, catalog, payment gateway, and notification capabilities remain external HTTP integrations served by WireMock in development and integration tests. All mutable endpoints require `Idempotency-Key`; all requests require `Correlation-Id`; all protected endpoints require JWT scopes.

**Scale/Scope**: Initial service scope covers order and payment APIs, core domain state machine, PostgreSQL persistence, external HTTP adapters, observability, Docker Compose infrastructure, and CI/CD validation.

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- Scope isolation: PASS. Plan implements only `order-service`; other contexts are external HTTP dependencies.
- No production mocks: PASS. WireMock is used only as an external standalone server/container for local and test environments.
- Real external communication: PASS. Infrastructure adapters call customer, catalog, payment, and notification endpoints through HTTP clients.
- Clean Architecture: PASS. Source layout separates domain, application, infrastructure, and interfaces.
- Quality gates: PASS. Domain unit coverage target is at least 80%; Pitest MSI target is at least 75%; integration tests use Testcontainers for PostgreSQL and WireMock.
- Idempotency and errors: PASS. Mutable operations require idempotency persistence; API errors use RFC 7807.
- Concurrency: PASS. PostgreSQL transactions and optimistic locking with version columns protect aggregate writes.
- Observability and security: PASS. JSON logs, correlation propagation, Micrometer/OpenTelemetry, JWT authentication, and scope authorization are planned.

Post-design re-check: PASS. `research.md`, `data-model.md`, `contracts/openapi.yaml`, and `quickstart.md` preserve the same constraints and introduce no constitution violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-order-management-service/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- openapi.yaml
|-- checklists/
|   `-- requirements.md
`-- tasks.md
```

### Source Code (repository root)

```text
docs/
`-- architecture.md

order-service/
|-- pom.xml
|-- Dockerfile
|-- src/main/java/org/raimundo/ecommerce/
|   |-- EcommerceApplication.java
|   |-- domain/
|   |   |-- order/
|   |   |-- payment/
|   |   |-- common/
|   |   `-- events/
|   |-- application/
|   |   |-- order/
|   |   |-- payment/
|   |   |-- ports/
|   |   `-- idempotency/
|   |-- infrastructure/
|   |   |-- persistence/
|   |   |-- http/
|   |   |-- security/
|   |   |-- observability/
|   |   `-- config/
|   `-- interfaces/
|       |-- rest/
|       |-- problem/
|       `-- dto/
|-- src/main/resources/
|   |-- application.properties
|   |-- db/migration/
|   `-- logback-spring.xml
`-- src/test/java/org/raimundo/ecommerce/
    |-- domain/
    |-- application/
    |-- integration/
    |-- contract/
    `-- support/

wiremock/
|-- mappings/
|   |-- customers-active.json
|   |-- customers-blocked.json
|   |-- customers-not-found.json
|   |-- products-available.json
|   |-- products-unavailable.json
|   |-- products-not-found.json
|   |-- payments-approved.json
|   |-- payments-rejected.json
|   |-- payments-gateway-unavailable.json
|   `-- notifications.json
`-- __files/

docker-compose.yml
README.md

.github/workflows/
`-- ci.yml
```

**Structure Decision**: Use the existing single Maven/Spring project and package by Clean Architecture layer. Domain code remains framework-free; application use cases depend on domain and ports; infrastructure implements persistence, HTTP clients, security, and observability; interfaces expose REST controllers and Problem Details mapping.

## Implementation Strategy

- Domain: model `Order` as the aggregate root with state transitions for `DRAFT`, `CONFIRMED`, `PAYMENT_PENDING`, `PAYMENT_APPROVED`, `PAYMENT_REJECTED`, and `CANCELLED`. Enforce active-order, editable-order, item quantity, confirmation, cancellation, payment retry, and duplicate event rules inside domain methods. A rejected payment increments the order payment rejection counter. The order may retry payment while it has fewer than 3 rejected attempts. After the third rejection, the order is automatically transitioned to `CANCELLED` with a cancellation reason such as `PAYMENT_RETRY_LIMIT_EXCEEDED`.
- Application: implement use cases for create order, get order, search by customer, add item, remove item, confirm order, cancel order, initiate payment, get payment, and process payment callback. Each mutable use case validates `Idempotency-Key`, opens a transaction, loads the aggregate with optimistic locking, executes domain behavior, persists changes, and records/reuses the idempotent response. The payment callback use case must detect duplicate gateway events, persist processed event identifiers, update the payment attempt status, increment the rejection counter on rejected payments, allow a new payment attempt while the counter is below 3, and automatically cancel the order when the third rejection is processed.
- Persistence: map orders, order items, payment transactions, processed payment events, and idempotency records with JPA. Add version columns to aggregates and unique indexes for active order per customer, item per product/order, idempotency identity, and payment event identity.
- Integrations: create outbound ports for customer validation, product validation/pricing, payment initiation, and notification publishing. Infrastructure adapters use WebClient with correlation propagation, timeouts, retries/circuit breakers where safe, and no production stub logic. The payment gateway adapter must handle at least three WireMock scenarios: approved payment (`200 APPROVED`), rejected payment (`200 REJECTED`), and temporary gateway instability (`503 Service Unavailable`). A `503` response must not approve, reject, or cancel the order by itself; it should be translated into a transient integration failure, preserving the current order/payment state and allowing the caller to retry according to idempotency and resilience rules.
- Security: configure OAuth2 Resource Server JWT validation. Protect order reads with `orders:read`, order writes with `orders:write`, payment reads with `payments:read`, and payment writes/callbacks with `payments:write`.
- Observability: add a request filter for `Correlation-Id`, structured JSON logging, Micrometer metrics for request outcomes/business errors/payment attempts/state transitions/external dependencies, OpenTelemetry tracing, Prometheus scraping, Grafana dashboard provisioning, and Jaeger tracing in Compose.
- Error handling: translate domain/application exceptions to RFC 7807 Problem Details with stable `type`, `title`, `status`, `detail`, and `instance` values.
- Infrastructure: provide Dockerfile, Docker Compose for order-service, PostgreSQL, WireMock, Prometheus, Grafana, Jaeger, and optional Keycloak/JWK support for local JWT validation.
- CI/CD: GitHub Actions runs build, unit tests, integration tests, Pitest mutation testing, Trivy security scan, and artifact/container packaging.

## Payment Test Strategy

The payment test suite must explicitly validate the domain retry limit and gateway instability behavior:

- A rejected payment increments the order rejection counter.
- The order remains eligible for payment retry after the first rejection.
- The order remains eligible for payment retry after the second rejection.
- The third rejected payment automatically cancels the order with reason `PAYMENT_RETRY_LIMIT_EXCEEDED`.
- Duplicate payment callback events are ignored after the first successful processing.
- A payment gateway `503 Service Unavailable` response is treated as a transient technical failure.
- A `503` response does not increment the rejection counter.
- A `503` response does not transition the order to `PAYMENT_REJECTED` or `CANCELLED`.

## Complexity Tracking

No constitution violations require justification.
