# E-Commerce Order Service

This repository contains the `order-service` implementation for the order and payment lifecycle.

## Local Services

```powershell
docker compose up -d postgres wiremock prometheus grafana jaeger
```

## Run

```powershell
.\mvnw -f order-service\pom.xml spring-boot:run
```

## Validate

```powershell
.\mvnw -f order-service\pom.xml test
.\mvnw -f order-service\pom.xml verify
.\mvnw -f order-service\pom.xml org.pitest:pitest-maven:mutationCoverage
```

Mutable API calls require `Correlation-Id`, `Idempotency-Key`, and a JWT with the matching scope:
`orders:read`, `orders:write`, `payments:read`, or `payments:write`.
