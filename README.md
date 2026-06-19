# E-Commerce Order Service

Backend do desafio "Plataforma de Pedidos para E-commerce". O repositorio implementa apenas o `order-service`; os demais servicos do ecossistema sao simulados por WireMock, conforme exigido pelo enunciado.

## Visao Geral

O `order-service` cobre o fluxo principal de pedidos:

- criar pedido para cliente existente e ativo;
- adicionar e remover itens enquanto o pedido esta em rascunho;
- confirmar pedido nao vazio usando o preco atual do catalogo;
- cancelar pedido antes da aprovacao do pagamento;
- iniciar pagamento;
- processar aprovacao/rejeicao de pagamento;
- cancelar automaticamente apos 3 rejeicoes de pagamento;
- proteger endpoints com JWT e escopos;
- propagar `Correlation-Id`;
- retornar erros no formato RFC 7807 Problem Details.

## Arquitetura

A aplicacao segue Clean Architecture / Arquitetura Hexagonal:

- `domain`: regras de negocio, agregados e value objects.
- `application`: casos de uso, portas e idempotencia.
- `infrastructure`: JPA, WebClient, seguranca, observabilidade e persistencia.
- `interfaces`: controllers REST, DTOs e Problem Details.

Mais detalhes estao em [`docs/architecture.md`](docs/architecture.md).

## Stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA
- Spring Security OAuth2 Resource Server
- PostgreSQL
- Flyway
- WebClient
- WireMock
- Testcontainers
- Micrometer + Prometheus
- OpenTelemetry
- Pitest
- Docker Compose
- GitHub Actions
- Trivy

## Servicos no Docker Compose

| Servico       | URL local                | Uso                                                  |
| ------------- | ------------------------ | ---------------------------------------------------- |
| order-service | `http://localhost:8080`  | API principal                                        |
| WireMock      | `http://localhost:8089`  | Simula clientes, catalogo, pagamentos e notificacoes |
| PostgreSQL    | `localhost:5432`         | Banco transacional                                   |
| Keycloak      | `http://localhost:8081`  | Identity provider local                              |
| Prometheus    | `http://localhost:9090`  | Metricas                                             |
| Grafana       | `http://localhost:3000`  | Dashboards locais                                    |
| Jaeger        | `http://localhost:16686` | UI de tracing local                                  |

## Como Rodar Localmente

Suba toda a stack:

```powershell
docker compose up --build
```

Ou suba dependencias e rode a aplicacao pelo Maven:

```powershell
docker compose up -d postgres wiremock prometheus grafana jaeger keycloak
.\mvnw -f order-service\pom.xml spring-boot:run
```

A aplicacao fica disponivel em:

```text
http://localhost:8080
```

## Documentacao da API

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI 3.1:

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/openapi.yaml
```

## Autenticacao e Headers

Os endpoints de negocio exigem JWT Bearer Token com escopos.

Escopos aceitos:

| Operacao            | Escopos                             |
| ------------------- | ----------------------------------- |
| Ler pedidos         | `orders:read` ou `order:read`       |
| Escrever pedidos    | `orders:write` ou `order:write`     |
| Ler pagamentos      | `payments:read` ou `payment:read`   |
| Escrever pagamentos | `payments:write` ou `payment:write` |

Headers obrigatorios para chamadas mutaveis:

```http
Authorization: Bearer <token>
Correlation-Id: <correlation-id>
Idempotency-Key: <idempotency-key>
```

Chamadas `GET` tambem exigem `Correlation-Id` e JWT com escopo de leitura.

## Endpoints Principais

Pedidos:

- `POST /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `GET /api/v1/orders?customerId={id}`
- `POST /api/v1/orders/{orderId}/items`
- `DELETE /api/v1/orders/{orderId}/items/{itemId}`
- `POST /api/v1/orders/{orderId}/confirm`
- `DELETE /api/v1/orders/{orderId}`

Pagamentos:

- `POST /api/v1/payments`
- `GET /api/v1/payments/{paymentId}`
- `POST /api/v1/payments/{paymentId}/callback`

## Cenarios WireMock

Os mappings ficam em [`wiremock/mappings`](wiremock/mappings).

Clientes:

- `customers-active.json`
- `customers-blocked.json`
- `customers-inactive.json`
- `customers-not-found.json`

Produtos:

- `products-available.json`
- `products-unavailable.json`
- `products-not-found.json`

Pagamentos:

- `payments-approved.json`
- `payments-rejected.json`
- `payments-gateway-unavailable.json`

Notificacoes:

- `notifications.json`

## Exemplos de Payload

Criar pedido:

```json
{
  "customerId": "customer-active"
}
```

Adicionar item:

```json
{
  "productId": "product-available",
  "quantity": 2
}
```

Iniciar pagamento:

```json
{
  "orderId": "00000000-0000-0000-0000-000000000000"
}
```

Callback de pagamento:

```json
{
  "providerEventId": "evt-123",
  "outcome": "APPROVED",
  "providerTransactionId": "provider-123",
  "detail": "approved"
}
```

## Validacao

Rodar todos os testes:

```powershell
.\mvnw -f order-service\pom.xml test
```

Rodar verificacao Maven:

```powershell
.\mvnw -f order-service\pom.xml verify
```

Rodar mutation testing no dominio:

```powershell
.\mvnw -f order-service\pom.xml org.pitest:pitest-maven:mutationCoverage
```

Build da imagem:

```powershell
docker build -t order-service:local -f order-service\Dockerfile .
```

## CI/CD

O workflow em [`.github/workflows/ci.yml`](.github/workflows/ci.yml) executa:

- checkout;
- setup do Java 21;
- testes;
- `verify`;
- Pitest;
- build Docker;
- scan de vulnerabilidades com Trivy.

## Observabilidade

Endpoints Actuator expostos:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
```

Recursos implementados:

- logs estruturados em JSON;
- `Correlation-Id` em requests, responses e chamadas externas;
- metricas Micrometer/Prometheus;
- OpenTelemetry com exportacao para logs;
- Compose com Prometheus, Grafana e Jaeger.

## Observacoes Conhecidas

- A Swagger UI usa assets via CDN.
- O Jaeger sobe no Compose, mas a aplicacao exporta traces para logging por padrao.
- Rate limiting explicito ainda nao esta implementado.
