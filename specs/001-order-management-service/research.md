# Research: E-Commerce Order Management

## Java 21 and Spring Boot 4.x

- Decision: Use the existing Java 21 and Spring Boot 4.1.0 Maven project as the service baseline.
- Rationale: The repository already declares Java 21 and Spring Boot 4.1.0 in `pom.xml`, matching the feature input. This keeps the plan aligned with the existing project instead of introducing a parallel stack.
- Alternatives considered: Spring Boot 3.x was rejected because the project already targets 4.x; another JVM framework was rejected because the constitution and input require Spring ecosystem behavior.

## Clean Architecture Package Layout

- Decision: Organize code into `domain`, `application`, `infrastructure`, and `interfaces` packages under `org.raimundo.ecommerce`.
- Rationale: This directly satisfies the constitution. Domain remains independent of Spring and persistence details; application ports define integration boundaries; infrastructure implements adapters; interfaces expose HTTP.
- Alternatives considered: Package-by-technical-controller/service/repository was rejected because it weakens domain isolation. Multi-module Maven was deferred because the current repository is a single service and the feature does not need module-level complexity.

## Persistence and Concurrency

- Decision: Use PostgreSQL with Spring Data JPA, Flyway migrations, optimistic locking, and transactional use cases.
- Rationale: PostgreSQL is required for transactional state, and optimistic locking fits aggregate-level write contention while keeping the service simple. Unique indexes enforce one active order per customer, one item per product in an order, idempotency uniqueness, and processed event uniqueness.
- Alternatives considered: Pessimistic locking was rejected for the initial scope because it increases blocking and operational complexity. In-memory or document storage was rejected because the constitution requires PostgreSQL/Testcontainers.

## Idempotency

- Decision: Persist idempotency records for every mutable operation keyed by operation scope, resource identity when applicable, caller identity, and `Idempotency-Key`, with the prior response reused for exact replays.
- Rationale: The feature requires repeat-safe payment initiation and callbacks, and the constitution requires native idempotency on mutable operations. Persisting the normalized request hash and response prevents duplicate side effects and detects conflicting reuse of a key.
- Alternatives considered: Controller-only cache was rejected because it fails across restarts and multiple instances. Payment-only idempotency was rejected because all mutable endpoints must support it.

## External Integrations and WireMock

- Decision: Implement customer, catalog, payment, and notification dependencies as outbound HTTP ports backed by WebClient adapters; use WireMock standalone mappings in `wiremock/mappings` for local and integration environments.
- Rationale: The constitution forbids production mocks but requires real HTTP communication with WireMock in development/test. Reusing the same mapping files in Docker Compose and Testcontainers keeps local and CI behavior consistent.
- Alternatives considered: Feign clients were rejected in favor of WebClient because the input explicitly requires WebClient. Production fake clients were rejected by constitution.

## Resilience

- Decision: Use Resilience4j timeouts, retries, and circuit breakers on outbound HTTP adapters. Retries apply only to safe validation/notification interactions or idempotent payment initiation where the same idempotency identity is sent.
- Rationale: External dependencies can be temporarily unavailable, but retries must not create duplicate payment side effects.
- Alternatives considered: Unbounded retries were rejected due to latency and duplicate-side-effect risk. No resilience was rejected because the feature includes payment-provider unavailability.

## Security

- Decision: Use Spring Security OAuth2 Resource Server with JWT bearer tokens and explicit scopes: `orders:read`, `orders:write`, `payments:read`, and `payments:write`.
- Rationale: This matches the constitution and the feature requirements for read/write authorization distinctions.
- Alternatives considered: API keys or session security were rejected because the project requires JWT/OAuth2 scope validation.

## Observability

- Decision: Propagate `Correlation-Id` through request handling, persistence audit fields, logs, external HTTP headers, metrics, and traces. Use JSON logs, Micrometer, OpenTelemetry, Prometheus, Grafana, and Jaeger.
- Rationale: The feature requires traceable operations and visibility into request outcomes, business errors, payments, state transitions, and external dependencies.
- Alternatives considered: Plain text logs and metrics-only visibility were rejected because they do not satisfy structured logging and distributed tracing requirements.

## Testing Strategy

- Decision: Use domain unit tests for rules and state transitions, application tests for use-case/idempotency behavior, integration tests with PostgreSQL and WireMock Testcontainers, contract tests against OpenAPI, and Pitest for mutation validation.
- Rationale: This satisfies the constitution's coverage, mutation, and Testcontainers gates while keeping each risk covered at the right layer.
- Alternatives considered: Controller-only integration tests were rejected because they would leave domain rules and mutation quality under-specified.
