# Order Service Architecture

Este documento descreve a arquitetura implementada para o desafio "Plataforma de Pedidos para E-commerce". Ele foi escrito a partir do escopo do `desafio.md`, da constituicao tecnica do projeto, do plano de implementacao e, principalmente, do que existe hoje no codigo de producao e nos testes.

## 1. Decisoes Centrais

O projeto implementa apenas o `order-service`. Os demais servicos do ecossistema de e-commerce sao tratados como dependencias externas e simulados por WireMock em ambiente local e nos testes de integracao. Essa decisao esta alinhada ao `desafio.md`, que exige que somente o servico de pedidos seja implementado de verdade, sem stubs ou mocks embutidos no codigo de producao.

As principais decisoes implementadas sao:

- Arquitetura em camadas no estilo Clean Architecture / Hexagonal Architecture.
- Dominio isolado de Spring, JPA e HTTP.
- Casos de uso na camada de aplicacao.
- Portas de saida para clientes, catalogo, pagamento e notificacao.
- Adaptadores HTTP reais com WebClient para os servicos externos.
- Persistencia relacional com PostgreSQL, JPA e Flyway.
- Testes de integracao com PostgreSQL e WireMock via Testcontainers.
- JWT Bearer Token com autorizacao baseada em escopos.
- Erros padronizados com Problem Details.
- Logs estruturados, Correlation-Id e metricas via Actuator/Micrometer.

## 2. Decomposicao do Dominio

Embora apenas o `order-service` tenha implementacao real, o dominio foi dividido em contextos que existiriam em uma arquitetura de marketplace.

### Order Service

Responsavel pelo ciclo de vida do pedido:

- criar pedido em rascunho;
- impedir mais de um pedido ativo para o mesmo cliente;
- adicionar e remover itens;
- impedir alteracao de pedidos confirmados ou cancelados;
- confirmar pedido com precos atuais do catalogo;
- cancelar pedido antes da aprovacao do pagamento;
- manter o estado do pedido durante o fluxo de pagamento;
- registrar dados de auditoria e correlacao.

No codigo, esse contexto aparece principalmente em:

- `domain/order`: agregado `Order`, `OrderItem`, `OrderStatus` e regras de transicao;
- `application/order`: casos de uso do pedido;
- `infrastructure/persistence/order`: entidades e repositorios JPA;
- `interfaces/rest/OrderController`: API HTTP versionada em `/api/v1/orders`.

### Customer Service

Responsavel por informar se um cliente existe, esta ativo e pode comprar. Ele nao e implementado no projeto. O `order-service` consome esse contexto por HTTP por meio de `CustomerPort` e `CustomerHttpAdapter`.

O WireMock cobre cenarios obrigatorios do desafio:

- cliente ativo;
- cliente bloqueado;
- cliente inativo;
- cliente nao encontrado.

Alinhamento com o `desafio.md`: um pedido so pode ser criado para cliente existente e ativo; clientes bloqueados, inativos ou inexistentes sao rejeitados antes do fluxo de pagamento.

### Catalog / Inventory Service

Responsavel por validar existencia, disponibilidade e preco atual de produtos. Ele tambem e externo ao `order-service` e e consumido por `CatalogPort` e `CatalogHttpAdapter`.

O WireMock cobre:

- produto disponivel com preco;
- produto indisponivel;
- produto nao encontrado.

Alinhamento com o `desafio.md`: item so pode ser adicionado se o produto existir e estiver disponivel. Na confirmacao, o preco e consultado novamente para que o total do pedido use o preco do momento da confirmacao, nao o preco do momento de adicao.

### Payment Gateway

Responsavel por iniciar e retornar o resultado de pagamentos. No projeto, ele e externo e simulado por WireMock. O `order-service` consome o gateway por `PaymentGatewayPort` e `PaymentGatewayHttpAdapter`.

O WireMock cobre:

- pagamento aprovado;
- pagamento rejeitado;
- gateway indisponivel.

Alinhamento com o `desafio.md`: o pagamento so inicia para pedido confirmado, rejeicoes permitem novas tentativas, e a terceira rejeicao cancela automaticamente o pedido.

### Notification Service

Responsavel por receber notificacoes de eventos relevantes, como pedido confirmado, pedido cancelado e pagamento aprovado. E consumido por `NotificationPort` e `NotificationHttpAdapter`.

O WireMock cobre:

- notificacao aceita com HTTP 202.

Alinhamento com o `desafio.md`: embora o desafio nao exija implementacao real do servico de notificacao, ele aparece na tabela minima de servicos externos e foi tratado como dependencia HTTP real.

## 3. Bounded Contexts e Relacionamentos

Os bounded contexts foram definidos assim:

| Contexto            | Responsabilidade                                | Implementacao                                                                      |
| ------------------- | ----------------------------------------------- | ---------------------------------------------------------------------------------- |
| Orders              | Ciclo de vida do pedido, itens, estados e total | Implementado no `order-service`                                                    |
| Payments            | Tentativas, status e callbacks de pagamento     | Implementado parcialmente dentro do `order-service` como parte do fluxo de pedidos |
| Customers           | Elegibilidade do cliente                        | Externo via WireMock                                                               |
| Catalog / Inventory | Disponibilidade e preco de produtos             | Externo via WireMock                                                               |
| Notifications       | Publicacao de eventos operacionais              | Externo via WireMock                                                               |
| Identity Provider   | Emissao e validacao de JWT                      | Externo, com Keycloak local no Docker Compose                                      |

O relacionamento entre contextos e sincrono por HTTP nesta entrega. Essa escolha simplifica a execucao local e os testes, e atende ao requisito do desafio de usar WireMock standalone para os servicos nao implementados.

Eventos de dominio foram modelados conceitualmente como mudancas de estado e chamadas de notificacao:

- `OrderConfirmed`;
- `OrderCancelled`;
- `PaymentApproved`;
- `PaymentRejected`.

Na implementacao atual, esses eventos nao sao publicados em um broker. Eles aparecem como transicoes no dominio e chamadas HTTP ao Notification Service. Essa foi uma escolha pragmatica para manter o escopo dentro de um unico servico implementado.

## 4. Clean Architecture / Arquitetura Hexagonal

O codigo esta organizado por camadas:

```text
order-service/src/main/java/org/raimundo/ecommerce/
|-- domain/
|   |-- common/
|   |-- order/
|   `-- payment/
|-- application/
|   |-- idempotency/
|   |-- order/
|   |-- payment/
|   `-- ports/
|-- infrastructure/
|   |-- http/
|   |-- observability/
|   |-- persistence/
|   `-- security/
`-- interfaces/
    |-- dto/
    |-- problem/
    `-- rest/
```

### Domain

Contem regras de negocio puras:

- `Order` controla estados e transicoes;
- `OrderItem` controla quantidade e total de linha;
- `Money` representa valores monetarios;
- `PaymentTransaction` representa uma tentativa de pagamento;
- `PaymentResultEvent` representa eventos de callback ja recebidos;
- `DomainException` representa violacoes de regra.

A camada de dominio nao depende de Spring, JPA, WebClient ou banco de dados. Isso atende ao requisito de separacao entre dominio e infraestrutura.

### Application

Contem os casos de uso e portas:

- `OrderService`: criar, buscar, listar, adicionar item, remover item, confirmar e cancelar pedido;
- `PaymentService`: iniciar pagamento, consultar pagamento e processar callback;
- `IdempotencyService`: validacao e replay de chaves de idempotencia nos fluxos em que o replay esta persistido;
- portas `CustomerPort`, `CatalogPort`, `PaymentGatewayPort` e `NotificationPort`.

A camada de aplicacao orquestra transacoes, chama portas externas e persiste agregados por interfaces.

### Infrastructure

Contem detalhes tecnicos:

- adaptadores JPA para pedidos, pagamentos e idempotencia;
- adaptadores HTTP com WebClient;
- configuracao JWT/OAuth2 Resource Server;
- filtro de Correlation-Id;
- metricas;
- logging estruturado.

### Interfaces

Contem a borda HTTP:

- `OrderController`;
- `PaymentController`;
- DTOs de request/response;
- `ProblemExceptionHandler` para RFC 7807.

As rotas sao versionadas com `/api/v1`, atendendo ao requisito de versionamento da API.

## 5. Ciclo de Vida do Pedido

O pedido usa estados explicitos:

| Estado             | Significado                                                           |
| ------------------ | --------------------------------------------------------------------- |
| `DRAFT`            | Pedido criado, ainda editavel                                         |
| `CONFIRMED`        | Pedido confirmado, itens congelados e total calculado                 |
| `PAYMENT_PENDING`  | Pagamento iniciado e aguardando resultado                             |
| `PAYMENT_APPROVED` | Pagamento aprovado                                                    |
| `PAYMENT_REJECTED` | Pagamento rejeitado, ainda permite nova tentativa se abaixo do limite |
| `CANCELLED`        | Pedido cancelado manualmente ou por limite de rejeicoes               |

Regras implementadas e alinhadas ao `desafio.md`:

- pedido recem-criado possui `customerId`;
- pedido so e confirmado com ao menos um item;
- preco e recalculado no momento da confirmacao;
- pedido confirmado nao aceita adicao ou remocao de itens;
- pedido cancelado nao pode ser modificado;
- pedido aprovado no pagamento nao pode ser cancelado;
- cliente pode ter no maximo um pedido ativo por vez;
- mesmo produto adicionado novamente incrementa quantidade em vez de duplicar item;
- remocao de item inexistente retorna erro de dominio;
- apos tres pagamentos rejeitados, o pedido e cancelado automaticamente.

## 6. Pagamentos e Idempotencia

O pagamento e iniciado por `POST /api/v1/payments` para pedidos confirmados. Cada pagamento gera uma `PaymentTransaction` com status e numero de tentativa.

Callbacks de pagamento sao recebidos em `POST /api/v1/payments/{id}/callback`. A idempotencia do webhook e protegida por `providerEventId`, persistido em `payment_result_events` com chave unica. Se o mesmo evento chegar novamente, ele nao gera novo efeito colateral.

Regras implementadas:

- pagamento so inicia para pedido `CONFIRMED` ou `PAYMENT_REJECTED`;
- pedido vai para `PAYMENT_PENDING` durante a tentativa;
- pagamento aprovado muda pedido para `PAYMENT_APPROVED`;
- pagamento rejeitado incrementa contador de rejeicoes;
- primeira e segunda rejeicoes deixam o pedido em estado retryable;
- terceira rejeicao cancela o pedido;
- callback repetido com mesmo evento e ignorado.

Idempotencia HTTP:

- `POST /api/v1/orders` persiste e reusa resposta por `Idempotency-Key`;
- `DELETE /api/v1/orders/{id}` persiste e reusa resposta por `Idempotency-Key`;
- `POST /api/v1/payments` reusa transacao pelo `idempotencyKey`;

## 7. Persistencia e Consistencia

O sistema usa banco relacional e migrations versionadas com Flyway. A migration inicial cria:

- `orders`;
- `order_items`;
- `payment_transactions`;
- `payment_result_events`;
- `idempotency_records`.

O controle de concorrencia usa optimistic locking com `@Version` em entidades de pedido e pagamento, alem de constraints no banco.

Constraints relevantes:

- indice unico parcial para impedir mais de um pedido ativo por cliente;
- unicidade de produto por pedido em `order_items`;
- unicidade de `idempotency_key` em `payment_transactions`;
- unicidade de `provider_event_id` em `payment_result_events`;
- unicidade da identidade de idempotencia em `idempotency_records`.

Alinhamento com o `desafio.md`: a estrategia escolhida para concorrencia e optimistic locking, apoiada por transacoes e constraints relacionais.

## 8. Contratos HTTP e WireMock

O `order-service` se comunica com dependencias externas por HTTP real. Os endpoints base sao configuraveis por propriedades:

- `external.customer-base-url`;
- `external.catalog-base-url`;
- `external.payment-base-url`;
- `external.notification-base-url`.

Em ambiente local, o Docker Compose sobe WireMock e monta `wiremock/mappings`. Nos testes de integracao, Testcontainers sobe PostgreSQL e WireMock reutilizando os mesmos mappings do repositorio.

Mapeamentos implementados:

- `customers-active.json`;
- `customers-blocked.json`;
- `customers-inactive.json`;
- `customers-not-found.json`;
- `products-available.json`;
- `products-unavailable.json`;
- `products-not-found.json`;
- `payments-approved.json`;
- `payments-rejected.json`;
- `payments-gateway-unavailable.json`;
- `notifications.json`.

Esse desenho atende ao requisito do desafio de nao ter mocks de servicos externos no codigo de producao.

## 9. API Publica

Endpoints de pedidos:

- `POST /api/v1/orders`;
- `GET /api/v1/orders/{orderId}`;
- `GET /api/v1/orders?customerId={id}`;
- `POST /api/v1/orders/{orderId}/items`;
- `DELETE /api/v1/orders/{orderId}/items/{itemId}`;
- `POST /api/v1/orders/{orderId}/confirm`;
- `DELETE /api/v1/orders/{orderId}`.

Endpoints de pagamentos:

- `POST /api/v1/payments`;
- `GET /api/v1/payments/{paymentId}`;
- `POST /api/v1/payments/{paymentId}/callback`.

Alinhamento com o `desafio.md`: os endpoints obrigatorios foram implementados com prefixo versionado `/api/v1`.

A documentacao OpenAPI 3.1 esta disponivel em:

- `/v3/api-docs`;
- `/openapi.yaml`.

A Swagger UI esta disponivel em `/swagger-ui.html`.

## 10. Tratamento de Erros

Erros de dominio, validacao, idempotencia, autorizacao e excecoes genericas sao convertidos para `ProblemDetail`, seguindo o formato RFC 7807.

As respostas incluem:

- `type`;
- `title`;
- `status`;
- `detail`;
- `code`;
- `correlationId`.

Alinhamento com o `desafio.md`: a API padroniza erros em Problem Details para os principais fluxos de erro testados.

## 11. Seguranca

O servico usa Spring Security OAuth2 Resource Server com JWT Bearer Token.

Escopos aplicados:

- leitura de pedidos: `orders:read` ou `order:read`;
- escrita de pedidos: `orders:write` ou `order:write`;
- leitura de pagamentos: `payments:read` ou `payment:read`;
- escrita de pagamentos: `payments:write` ou `payment:write`.

O Docker Compose inclui Keycloak local para apoiar autenticacao em ambiente de desenvolvimento.

Alinhamento com o `desafio.md`: endpoints protegidos por JWT e controle por escopo foram implementados.

Limite conhecido: controles OWASP adicionais, como rate limiting explicito e headers de seguranca customizados, nao estao evidenciados na implementacao atual.

## 12. Observabilidade

Recursos implementados:

- logs estruturados em JSON via Logstash Encoder;
- `Correlation-Id` obrigatorio em requests;
- propagacao de `Correlation-Id` para chamadas externas;
- armazenamento de correlacao em registros persistidos relevantes;
- metricas expostas pelo Spring Actuator e Micrometer;
- endpoint Prometheus em `/actuator/prometheus`;
- OpenTelemetry configurado com exportacao para logging;
- Docker Compose com Prometheus, Grafana e Jaeger.

Alinhamento com o `desafio.md`: ha logs estruturados, correlacao, metricas e infraestrutura local de observabilidade.

Limite conhecido: Jaeger e iniciado no Docker Compose, mas a aplicacao esta configurada para exportar traces para logging, nao diretamente para Jaeger.

## 13. Testes e Qualidade

A suite implementada cobre:

- testes unitarios de dominio;
- testes de aplicacao;
- testes de integracao com PostgreSQL real via Testcontainers;
- testes de integracao com WireMock via Testcontainers;
- testes de seguranca;
- testes de Problem Details;
- testes de observabilidade e Correlation-Id.

O suporte de integracao usa containers singleton para PostgreSQL e WireMock, evitando que a suite inteira reutilize um contexto Spring apontando para portas de containers ja finalizados.

O Maven tambem possui configuracao de Pitest com mutation threshold de 75% para o pacote de dominio.

Alinhamento com o `desafio.md`: testes unitarios, integracao com banco real, integracao com WireMock/Testcontainers e mutation testing foram contemplados. A comprovacao final do MSI depende da execucao do comando Pitest no ambiente de avaliacao.

## 14. Containerizacao e CI/CD

O repositorio inclui:

- `Dockerfile` do `order-service`;
- `docker-compose.yml` com PostgreSQL, WireMock, Prometheus, Grafana, Jaeger, Keycloak e order-service;
- GitHub Actions com build, testes, Pitest, build Docker e Trivy.

Alinhamento com o `desafio.md`: a estrutura de entrega esperada esta presente e cobre os principais servicos de apoio.

## 15. Trade-offs

### HTTP sincrono em vez de mensageria

O desafio sugere pensar em eventos de negocio, mas nao exige broker. A implementacao usa chamadas HTTP e notificacoes sincronas para manter o projeto executavel com Docker Compose e WireMock. Em producao, eventos como `OrderConfirmed` e `PaymentApproved` poderiam ser publicados em Kafka, RabbitMQ ou outro broker.

### Payment dentro do `order-service`

O pagamento poderia ser um microservico separado. Nesta entrega, o fluxo de pagamento foi implementado dentro do `order-service` porque o desafio pede o fluxo completo de pedido e pagamento, mas limita a implementacao real ao servico de pedidos. O gateway em si permanece externo e simulado.

### Optimistic locking

Foi escolhida concorrencia otimista porque pedidos tendem a ter baixa contencao por agregado e porque `@Version` com constraints de banco resolve conflitos sem manter locks pessimistas longos. Essa escolha tambem simplifica testes e operacao local.

### WireMock como contrato executavel

Os mappings do WireMock funcionam como evidencia pratica dos contratos externos. Isso atende bem ao requisito de simulacao dos demais servicos.
