# [E-COMMERCE] Constitution

Este documento estabelece as leis arquiteturais, técnicas e de qualidade inegociáveis para o desenvolvimento do ecossistema. Qualquer código que viole estas diretrizes será rejeitado automaticamente.

## 1. RESTRIÇÕES DE ESCOPO E ECOSSISTEMA

- **Isolamento de Escopo:** Apenas o serviço de pedidos (`order-service`) terá implementação real de código.
- **Proibição de Mocks em Produção:** O código de produção não deve conter stubs, beans de mock ou emulações embutidas para serviços externos.
- **Comunicação Externa Real:** Toda integração externa deve ser feita via chamadas HTTP reais consumindo um servidor WireMock standalone (em ambiente de desenvolvimento/teste).

## 2. PONTOS ARQUITETURAIS (CLEAN ARCHITECTURE)

- **Arquitetura:** A arquitetura a ser seguida deve ser CLEAN ARCHITECTURE com separação clara entre Domínio (entidades, value objects, regras de negócio), Aplicação (casos de uso / use cases) e Infraestrutura (adaptadores: HTTP, banco de dados, clientes HTTP para os demais serviços)

## 3. GARANTIA DE QUALIDADE E COBERTURA (QA)

- **Métricas de Testes Unitários:** Mínimo de 80% de cobertura de código na camada de domínio.
- **Testes de Mutação:** Score mínimo de 75% (MSI) via Pitest (ou equivalente da linguagem escolhida) no módulo de domínio.
- **Testes de Integração:** Obrigatório o uso de Testcontainers para gerenciar as instâncias reais do banco de dados (PostgreSQL) e do WireMock nos ambientes de teste.

## 4. RESILIÊNCIA, IDEMPOTÊNCIA E TRATAMENTO DE ERROS

- **Idempotência Nativa:** Todas as operações mutáveis (POST, PUT, DELETE) devem suportar e validar chaves de idempotência na camada de transporte/aplicação.
- **Tratamento de Erros Padronizado:** Toda e qualquer resposta de erro da API deve seguir estritamente a especificação RFC 7807 (Problem Details).
- **Tratamento de Concorrência:** É obrigatória a implementação de um mecanismo explícito de controle de concorrência (ex: Optimistic ou Pessimistic Locking) para evitar Race Conditions em operações de escrita no mesmo recurso.

## 5. REQUISITOS DE OBSERVABILIDADE E SEGURANÇA

- **Logs Estruturados:** Todos os logs do sistema devem ser gerados em formato estruturado (JSON).
- **Rastreabilidade:** Obrigatória a propagação e logging de um `CorrelationID` único através de todas as camadas e chamadas externas.
- **Segurança Baseada em Escopo:** Todos os endpoints restritos devem exigir autenticação via JWT (OAuth2 / Bearer Token) com validação explícita de escopos (scopes).
