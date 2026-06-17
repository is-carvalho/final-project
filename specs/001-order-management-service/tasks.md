# Tasks: E-Commerce Order Management

**Input**: Design documents from `specs/001-order-management-service/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`, `quickstart.md`

**Tests**: Included because the specification, plan, and constitution require domain coverage, mutation testing, integration testing with PostgreSQL/WireMock Testcontainers, and API contract validation.

**Organization**: Tasks are grouped by user story so each story can be implemented, tested, and delivered independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare the Java 21/Spring Boot Maven project under `order-service/` for the order service implementation.

- [ ] T001 Update Spring Boot, persistence, security, WebClient, Resilience4j, observability, Testcontainers, OpenAPI, and Pitest dependencies/plugins in `order-service/pom.xml`
- [ ] T002 Create Clean Architecture package directories under `order-service/src/main/java/org/raimundo/ecommerce/domain`, `order-service/src/main/java/org/raimundo/ecommerce/application`, `order-service/src/main/java/org/raimundo/ecommerce/infrastructure`, and `order-service/src/main/java/org/raimundo/ecommerce/interfaces`
- [ ] T003 [P] Create matching test package directories under `order-service/src/test/java/org/raimundo/ecommerce/domain`, `order-service/src/test/java/org/raimundo/ecommerce/application`, `order-service/src/test/java/org/raimundo/ecommerce/integration`, `order-service/src/test/java/org/raimundo/ecommerce/contract`, and `order-service/src/test/java/org/raimundo/ecommerce/support`
- [ ] T004 [P] Add local PostgreSQL, WireMock, Prometheus, Grafana, Jaeger, and optional JWK/Keycloak service definitions in `docker-compose.yml`
- [ ] T005 [P] Create WireMock mapping directory structure in `wiremock/mappings` and `wiremock/__files`
- [ ] T006 [P] Add order-service Docker image build configuration in `order-service/Dockerfile`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that must exist before any user story can be implemented.

**CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T007 Create initial Flyway schema for orders, order_items, payment_transactions, payment_result_events, and idempotency_records in `order-service/src/main/resources/db/migration/V001__create_order_management_schema.sql`
- [ ] T008 Implement shared domain value objects and exceptions in `order-service/src/main/java/org/raimundo/ecommerce/domain/common`
- [ ] T009 Implement RFC 7807 exception mapping and problem response support in `order-service/src/main/java/org/raimundo/ecommerce/interfaces/problem`
- [ ] T010 Implement Correlation-Id request filter and context propagation support in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/observability`
- [ ] T011 Implement idempotency model, repository port, and service contract in `order-service/src/main/java/org/raimundo/ecommerce/application/idempotency`
- [ ] T012 Implement JPA idempotency persistence adapter in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/persistence/idempotency`
- [ ] T013 Implement outbound HTTP client configuration with WebClient, timeouts, Resilience4j, and correlation propagation in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/http`
- [ ] T014 Implement OAuth2 Resource Server JWT scope configuration in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/security/SecurityConfig.java`
- [ ] T015 Configure datasource, Flyway, external service base URLs, security issuer/JWK settings, actuator, metrics, and tracing in `order-service/src/main/resources/application.properties`
- [ ] T016 Configure structured JSON logging in `order-service/src/main/resources/logback-spring.xml`
- [ ] T017 [P] Add reusable PostgreSQL and WireMock Testcontainers support in `order-service/src/test/java/org/raimundo/ecommerce/support/IntegrationTestSupport.java`
- [ ] T018 [P] Add JWT and request header test helpers in `order-service/src/test/java/org/raimundo/ecommerce/support/ApiTestSupport.java`

**Checkpoint**: Foundation ready. User story implementation can now begin in priority order or in parallel by separate contributors.

---

## Phase 3: User Story 1 - Create and Maintain an Active Order (Priority: P1) MVP

**Goal**: Create an eligible customer's draft order and manage draft items while enforcing active-order, product, quantity, duplicate-item, and idempotency rules.

**Independent Test**: Create an order for an active customer, add and remove products, add the same product twice, and verify invalid customers, invalid products, duplicate active orders, and invalid quantities are rejected.

### Tests for User Story 1

- [ ] T019 [P] [US1] Add domain unit tests for draft order creation and item mutation rules in `order-service/src/test/java/org/raimundo/ecommerce/domain/order/OrderDraftTest.java`
- [ ] T020 [P] [US1] Add application tests for create order idempotency and duplicate active-order prevention in `order-service/src/test/java/org/raimundo/ecommerce/application/order/CreateOrderUseCaseTest.java`
- [ ] T021 [P] [US1] Add application tests for add/remove item validation and idempotency in `order-service/src/test/java/org/raimundo/ecommerce/application/order/ManageOrderItemsUseCaseTest.java`
- [ ] T022 [P] [US1] Add API contract tests for `POST /api/v1/orders`, `GET /api/v1/orders`, `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/items`, and `DELETE /api/v1/orders/{id}/items/{itemId}` in `order-service/src/test/java/org/raimundo/ecommerce/contract/OrderDraftContractTest.java`
- [ ] T023 [P] [US1] Add PostgreSQL/WireMock integration tests for create and edit draft order scenarios in `order-service/src/test/java/org/raimundo/ecommerce/integration/OrderDraftIntegrationTest.java`

### Implementation for User Story 1

- [ ] T024 [P] [US1] Implement order aggregate, item entity, statuses, and draft mutation behavior in `order-service/src/main/java/org/raimundo/ecommerce/domain/order`
- [ ] T025 [P] [US1] Implement customer and catalog outbound ports in `order-service/src/main/java/org/raimundo/ecommerce/application/ports`
- [ ] T026 [US1] Implement create order, get order, find orders, add item, and remove item use cases in `order-service/src/main/java/org/raimundo/ecommerce/application/order`
- [ ] T027 [US1] Implement order JPA entities, repositories, and mappers in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/persistence/order`
- [ ] T028 [US1] Implement customer and catalog WebClient adapters in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/http`
- [ ] T029 [US1] Implement order request/response DTOs in `order-service/src/main/java/org/raimundo/ecommerce/interfaces/dto`
- [ ] T030 [US1] Implement order REST controller endpoints for draft order operations in `order-service/src/main/java/org/raimundo/ecommerce/interfaces/rest/OrderController.java`
- [ ] T031 [US1] Add WireMock mappings for active, blocked, inactive, and missing customers in `wiremock/mappings/customers-active.json`, `wiremock/mappings/customers-blocked.json`, `wiremock/mappings/customers-inactive.json`, and `wiremock/mappings/customers-not-found.json`
- [ ] T032 [US1] Add WireMock mappings for available, unavailable, and missing products in `wiremock/mappings/products-available.json`, `wiremock/mappings/products-unavailable.json`, and `wiremock/mappings/products-not-found.json`

**Checkpoint**: User Story 1 is fully functional and testable as the MVP.

---

## Phase 4: User Story 2 - Confirm or Cancel an Order (Priority: P2)

**Goal**: Confirm valid non-empty draft orders with current pricing and allow cancellation before payment approval while rejecting invalid lifecycle transitions.

**Independent Test**: Confirm a non-empty order, verify totals are recalculated, verify later item changes are rejected, and validate allowed and disallowed cancellations.

### Tests for User Story 2

- [ ] T033 [P] [US2] Add domain unit tests for confirmation, cancellation, and invalid lifecycle transitions in `order-service/src/test/java/org/raimundo/ecommerce/domain/order/OrderLifecycleTest.java`
- [ ] T034 [P] [US2] Add application tests for confirm order pricing, notification, idempotency, and cancellation behavior in `order-service/src/test/java/org/raimundo/ecommerce/application/order/ConfirmCancelOrderUseCaseTest.java`
- [ ] T035 [P] [US2] Add API contract tests for `POST /api/v1/orders/{id}/confirm` and `DELETE /api/v1/orders/{id}` in `order-service/src/test/java/org/raimundo/ecommerce/contract/OrderLifecycleContractTest.java`
- [ ] T036 [P] [US2] Add integration tests for confirm, post-confirm item rejection, and cancellation scenarios in `order-service/src/test/java/org/raimundo/ecommerce/integration/OrderLifecycleIntegrationTest.java`

### Implementation for User Story 2

- [ ] T037 [US2] Extend order aggregate confirmation, pricing recalculation, cancellation reason, and lifecycle guard behavior in `order-service/src/main/java/org/raimundo/ecommerce/domain/order`
- [ ] T038 [P] [US2] Implement notification outbound port in `order-service/src/main/java/org/raimundo/ecommerce/application/ports/NotificationPort.java`
- [ ] T039 [US2] Implement confirm order and cancel order use cases in `order-service/src/main/java/org/raimundo/ecommerce/application/order`
- [ ] T040 [US2] Extend order persistence mapping for finalized prices, totals, cancellation fields, and optimistic locking in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/persistence/order`
- [ ] T041 [US2] Implement notification WebClient adapter in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/http/NotificationHttpAdapter.java`
- [ ] T042 [US2] Extend order REST controller for confirmation and cancellation in `order-service/src/main/java/org/raimundo/ecommerce/interfaces/rest/OrderController.java`
- [ ] T043 [US2] Add WireMock mapping for notification publishing in `wiremock/mappings/notifications.json`

**Checkpoint**: User Stories 1 and 2 work independently and together.

---

## Phase 5: User Story 3 - Process Payment Reliably (Priority: P3)

**Goal**: Initiate payments repeat-safely, process approval/rejection callbacks idempotently, allow retries before the third rejection, and automatically cancel after the third rejection.

**Independent Test**: Initiate payment for a confirmed order, repeat initiation with the same idempotency identity, replay approval/rejection callbacks, verify retry eligibility, and verify automatic cancellation after the third rejection.

### Tests for User Story 3

- [ ] T044 [P] [US3] Add domain unit tests for payment pending, approval, rejection retry, third-rejection cancellation, duplicate event handling, and gateway 503 preservation rules in `order-service/src/test/java/org/raimundo/ecommerce/domain/payment/PaymentLifecycleTest.java`
- [ ] T045 [P] [US3] Add application tests for initiate payment idempotency and callback replay behavior in `order-service/src/test/java/org/raimundo/ecommerce/application/payment/PaymentUseCaseTest.java`
- [ ] T046 [P] [US3] Add API contract tests for `POST /api/v1/payments`, `GET /api/v1/payments/{id}`, and `POST /api/v1/payments/{id}/callback` in `order-service/src/test/java/org/raimundo/ecommerce/contract/PaymentContractTest.java`
- [ ] T047 [P] [US3] Add integration tests for approved payment, rejected payment retries, third-rejection cancellation, duplicate callbacks, and gateway 503 behavior in `order-service/src/test/java/org/raimundo/ecommerce/integration/PaymentIntegrationTest.java`

### Implementation for User Story 3

- [ ] T048 [P] [US3] Implement payment transaction and payment result event domain models in `order-service/src/main/java/org/raimundo/ecommerce/domain/payment`
- [ ] T049 [US3] Extend order aggregate payment state transitions and rejection counter behavior in `order-service/src/main/java/org/raimundo/ecommerce/domain/order`
- [ ] T050 [P] [US3] Implement payment gateway outbound port in `order-service/src/main/java/org/raimundo/ecommerce/application/ports/PaymentGatewayPort.java`
- [ ] T051 [US3] Implement initiate payment, get payment, and process callback use cases in `order-service/src/main/java/org/raimundo/ecommerce/application/payment`
- [ ] T052 [US3] Implement payment JPA entities, repositories, event uniqueness, and mappers in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/persistence/payment`
- [ ] T053 [US3] Implement payment gateway WebClient adapter with approved, rejected, and 503 handling in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/http/PaymentGatewayHttpAdapter.java`
- [ ] T054 [US3] Implement payment DTOs in `order-service/src/main/java/org/raimundo/ecommerce/interfaces/dto`
- [ ] T055 [US3] Implement payment REST controller in `order-service/src/main/java/org/raimundo/ecommerce/interfaces/rest/PaymentController.java`
- [ ] T056 [US3] Add WireMock mappings for approved, rejected, and unavailable payment gateway responses in `wiremock/mappings/payments-approved.json`, `wiremock/mappings/payments-rejected.json`, and `wiremock/mappings/payments-gateway-unavailable.json`

**Checkpoint**: User Stories 1, 2, and 3 cover the complete order/payment lifecycle.

---

## Phase 6: User Story 4 - Operate Securely and Traceably (Priority: P4)

**Goal**: Enforce scopes, propagate correlation identifiers, expose operational visibility, and return consistent Problem Details responses.

**Independent Test**: Call protected endpoints with missing or insufficient scopes, verify correlation appears in logs/persistence/external calls, and confirm business errors follow RFC 7807.

### Tests for User Story 4

- [ ] T057 [P] [US4] Add security tests for order read/write and payment read/write scopes in `order-service/src/test/java/org/raimundo/ecommerce/integration/SecurityIntegrationTest.java`
- [ ] T058 [P] [US4] Add Problem Details tests for business, validation, idempotency, authorization, and integration errors in `order-service/src/test/java/org/raimundo/ecommerce/integration/ProblemDetailsIntegrationTest.java`
- [ ] T059 [P] [US4] Add observability tests for Correlation-Id propagation to persistence and outbound HTTP requests in `order-service/src/test/java/org/raimundo/ecommerce/integration/ObservabilityIntegrationTest.java`

### Implementation for User Story 4

- [ ] T060 [US4] Apply method or route-level scope authorization to order and payment controllers in `order-service/src/main/java/org/raimundo/ecommerce/interfaces/rest`
- [ ] T061 [US4] Complete Problem Details error type mapping for domain, application, idempotency, security, and external integration failures in `order-service/src/main/java/org/raimundo/ecommerce/interfaces/problem`
- [ ] T062 [US4] Add Micrometer metrics for request outcomes, business errors, payment attempts, state transitions, and external dependencies in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/observability`
- [ ] T063 [US4] Add OpenTelemetry trace propagation for inbound requests and outbound HTTP clients in `order-service/src/main/java/org/raimundo/ecommerce/infrastructure/observability`
- [ ] T064 [US4] Add Prometheus, Grafana, and Jaeger local configuration in `order-service/src/main/resources/observability`
- [ ] T065 [US4] Ensure audit and correlation fields are populated during writes in `order-service/src/main/java/org/raimundo/ecommerce/application`

**Checkpoint**: Security, traceability, and operator visibility are complete.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, quality gates, documentation, and delivery automation.

- [ ] T066 [P] Update architecture notes for Clean Architecture boundaries and external HTTP dependencies in `docs/architecture.md`
- [ ] T067 [P] Update local setup, API usage, WireMock scenarios, observability, and validation steps in `README.md`
- [ ] T068 Add GitHub Actions workflow for compile, unit tests, integration tests, OpenAPI contract tests, Pitest, Trivy scan, and container packaging in `.github/workflows/ci.yml`
- [ ] T069 Run and fix issues from `.\mvnw -f order-service\pom.xml test`
- [ ] T070 Run and fix issues from `.\mvnw -f order-service\pom.xml verify`
- [ ] T071 Run and fix issues from `.\mvnw -f order-service\pom.xml org.pitest:pitest-maven:mutationCoverage`
- [ ] T072 Validate quickstart scenarios from `specs/001-order-management-service/quickstart.md`
- [ ] T073 Verify generated implementation remains aligned with `specs/001-order-management-service/contracts/openapi.yaml`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion; blocks all user stories.
- **User Stories (Phases 3-6)**: Depend on Foundational completion.
- **Polish (Phase 7)**: Depends on all desired user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Starts after Foundation; no dependency on other stories.
- **User Story 2 (P2)**: Starts after Foundation and uses the order aggregate from US1.
- **User Story 3 (P3)**: Starts after Foundation and uses confirmed/cancellable order behavior from US2.
- **User Story 4 (P4)**: Starts after Foundation and can be developed alongside story endpoints, but final validation depends on US1-US3 endpoints existing.

### Within Each User Story

- Write the listed tests first and confirm they fail before implementation.
- Implement domain behavior before application use cases.
- Implement ports before infrastructure adapters.
- Implement persistence before REST endpoints that depend on stored state.
- Complete each story's checkpoint before moving to the next priority when working sequentially.

### Parallel Opportunities

- T003, T004, T005, and T006 can run in parallel after T001 establishes dependencies.
- T017 and T018 can run in parallel with foundational infrastructure work after T007-T015 are understood.
- Test tasks within each user story are parallelizable because they target separate files.
- Independent ports, adapters, DTOs, and WireMock mappings marked `[P]` can be implemented in parallel.
- After Phase 2, US1-US4 may be staffed in parallel, with coordination on shared order and payment files.

---

## Parallel Examples

### User Story 1

```text
Task: "T019 [P] [US1] Add domain unit tests for draft order creation and item mutation rules in order-service/src/test/java/org/raimundo/ecommerce/domain/order/OrderDraftTest.java"
Task: "T020 [P] [US1] Add application tests for create order idempotency and duplicate active-order prevention in order-service/src/test/java/org/raimundo/ecommerce/application/order/CreateOrderUseCaseTest.java"
Task: "T022 [P] [US1] Add API contract tests for POST /api/v1/orders, GET /api/v1/orders, GET /api/v1/orders/{id}, POST /api/v1/orders/{id}/items, and DELETE /api/v1/orders/{id}/items/{itemId} in order-service/src/test/java/org/raimundo/ecommerce/contract/OrderDraftContractTest.java"
```

### User Story 2

```text
Task: "T033 [P] [US2] Add domain unit tests for confirmation, cancellation, and invalid lifecycle transitions in order-service/src/test/java/org/raimundo/ecommerce/domain/order/OrderLifecycleTest.java"
Task: "T035 [P] [US2] Add API contract tests for POST /api/v1/orders/{id}/confirm and DELETE /api/v1/orders/{id} in order-service/src/test/java/org/raimundo/ecommerce/contract/OrderLifecycleContractTest.java"
Task: "T038 [P] [US2] Implement notification outbound port in order-service/src/main/java/org/raimundo/ecommerce/application/ports/NotificationPort.java"
```

### User Story 3

```text
Task: "T044 [P] [US3] Add domain unit tests for payment pending, approval, rejection retry, third-rejection cancellation, duplicate event handling, and gateway 503 preservation rules in order-service/src/test/java/org/raimundo/ecommerce/domain/payment/PaymentLifecycleTest.java"
Task: "T046 [P] [US3] Add API contract tests for POST /api/v1/payments, GET /api/v1/payments/{id}, and POST /api/v1/payments/{id}/callback in order-service/src/test/java/org/raimundo/ecommerce/contract/PaymentContractTest.java"
Task: "T050 [P] [US3] Implement payment gateway outbound port in order-service/src/main/java/org/raimundo/ecommerce/application/ports/PaymentGatewayPort.java"
```

### User Story 4

```text
Task: "T057 [P] [US4] Add security tests for order read/write and payment read/write scopes in order-service/src/test/java/org/raimundo/ecommerce/integration/SecurityIntegrationTest.java"
Task: "T058 [P] [US4] Add Problem Details tests for business, validation, idempotency, authorization, and integration errors in order-service/src/test/java/org/raimundo/ecommerce/integration/ProblemDetailsIntegrationTest.java"
Task: "T059 [P] [US4] Add observability tests for Correlation-Id propagation to persistence and outbound HTTP requests in order-service/src/test/java/org/raimundo/ecommerce/integration/ObservabilityIntegrationTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Validate US1 independently with domain, application, contract, and integration tests.
5. Demo draft order creation and item management.

### Incremental Delivery

1. Complete Setup and Foundational infrastructure.
2. Add US1 for editable draft orders.
3. Add US2 for confirmation and cancellation.
4. Add US3 for payment initiation, callbacks, retries, and automatic cancellation.
5. Add US4 for hardened security, traceability, and observability.
6. Finish Phase 7 quality gates and documentation.

### Parallel Team Strategy

1. Team completes Setup and Foundational tasks together.
2. After Phase 2, separate contributors can take US1 tests, persistence/adapters, and REST endpoints in parallel.
3. US2 and US3 should coordinate changes to `order-service/src/main/java/org/raimundo/ecommerce/domain/order`.
4. US4 can progress on security/problem/observability infrastructure while endpoint stories mature.

---

## Notes

- `[P]` tasks target different files or independent setup work.
- `[US1]` through `[US4]` labels map directly to user stories in `spec.md`.
- All mutable endpoints must require and persist `Idempotency-Key`.
- All API requests must require `Correlation-Id`.
- Production code must use real HTTP clients for customer, catalog, payment, and notification integrations; WireMock is only local/test infrastructure.
- Domain behavior must reach at least 80% coverage and 75% Pitest mutation score.
