# Order Service Architecture

`order-service` follows Clean Architecture boundaries:

- `domain`: framework-free order and payment state machines, value objects, and business exceptions.
- `application`: use cases, idempotency contracts, and outbound ports.
- `infrastructure`: JPA persistence, WebClient HTTP adapters, JWT security, metrics, logging, and correlation propagation.
- `interfaces`: REST controllers, DTOs, and RFC 7807 Problem Details mapping.

External customer, catalog, payment gateway, and notification capabilities are accessed through HTTP ports. Local and integration environments provide those dependencies with standalone WireMock mappings under `wiremock/mappings`.
