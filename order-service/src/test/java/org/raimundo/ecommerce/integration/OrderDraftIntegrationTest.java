package org.raimundo.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.support.ApiTestSupport;
import org.raimundo.ecommerce.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T023: Integration tests para criar e editar pedidos em rascunho (draft).
 * 
 * Testa cenários com PostgreSQL real e WireMock para validar:
 * - Criação de pedido para cliente ativo
 * - Adição e remoção de produtos
 * - Validação de cliente ativo/bloqueado/inativo
 * - Validação de produtos disponíveis/indisponíveis
 * - Prevenção de duplicata de pedidos ativos
 * - Validação de quantidades inválidas
 * - Idempotência de criação
 */
@DisplayName("T023: Draft Order Integration Tests (PostgreSQL + WireMock)")
class OrderDraftIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static final String CORRELATION_ID = "correlation-123";
    private static final String IDEMPOTENCY_KEY = "key-" + UUID.randomUUID();
    private static final String ACTIVE_CUSTOMER_ID = "customer-active";
    private static final String INACTIVE_CUSTOMER_ID = "customer-inactive";
    private static final String BLOCKED_CUSTOMER_ID = "customer-blocked";
    private static final String UNAVAILABLE_PRODUCT_ID = "product-unavailable";
    private static final String AVAILABLE_PRODUCT_ID = "product-available";

    @Test
    @DisplayName("Should create a draft order for an active customer")
    void testCreateDraftOrderForActiveCustomer() throws Exception {
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id", notNullValue()),
                        jsonPath("$.customerId", is(ACTIVE_CUSTOMER_ID)),
                        jsonPath("$.status", is("DRAFT")),
                        jsonPath("$.items", hasSize(0))
                );
    }

    @Test
    @DisplayName("Should add product to draft order")
    void testAddProductToDraftOrder() throws Exception {
        // Create draft order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = mapper.readTree(createResponse).get("id").asText(null) != null ? 
                UUID.fromString(mapper.readTree(createResponse).get("id").asText()) : null;

        // Add item to draft order
        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-item-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "productId": "%s",
                                  "quantity": 2
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.items", hasSize(1)),
                        jsonPath("$.items[0].quantity", is(2))
                );
    }

    @Test
    @DisplayName("Should reject creation for inactive customer")
    void testRejectInactiveCustomer() throws Exception {
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-inactive-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(INACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue())
                );
    }

    @Test
    @DisplayName("Should reject creation for blocked customer")
    void testRejectBlockedCustomer() throws Exception {
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-blocked-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(BLOCKED_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue())
                );
    }

    @Test
    @DisplayName("Should reject adding unavailable product")
    void testRejectUnavailableProduct() throws Exception {
        // Create draft order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = mapper.readTree(createResponse).get("id").asText(null) != null ? 
                UUID.fromString(mapper.readTree(createResponse).get("id").asText()) : null;

        // Try to add unavailable product
        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-unavail-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(UNAVAILABLE_PRODUCT_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue())
                );
    }

    @Test
    @DisplayName("Should reject invalid quantity")
    void testRejectInvalidQuantity() throws Exception {
        // Create draft order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = mapper.readTree(createResponse).get("id").asText(null) != null ? 
                UUID.fromString(mapper.readTree(createResponse).get("id").asText()) : null;

        // Try to add with zero quantity
        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-zero-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "productId": "%s",
                                  "quantity": 0
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue())
                );
    }

    @Test
    @DisplayName("Should remove item from draft order")
    void testRemoveItemFromDraftOrder() throws Exception {
        // Create draft order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = UUID.fromString(mapper.readTree(createResponse).get("id").asText());

        // Add item
        String addResponse = mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-item-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID itemId = UUID.fromString(mapper.readTree(addResponse).get("items").get(0).get("id").asText());

        // Remove item
        mvc.perform(delete("/api/v1/orders/{id}/items/{itemId}", orderId, itemId)
                        .header("Idempotency-Key", "key-remove-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.items", hasSize(0))
                );
    }

    @Test
    @DisplayName("Should increase quantity when adding same product twice")
    void testAddSameProductTwiceIncreasesQuantity() throws Exception {
        // Create draft order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = UUID.fromString(mapper.readTree(createResponse).get("id").asText());

        // Add same product twice with different quantities
        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-first-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "productId": "%s",
                                  "quantity": 2
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-second-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "productId": "%s",
                                  "quantity": 3
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.items", hasSize(1)),
                        jsonPath("$.items[0].quantity", is(5))
                );
    }

    @Test
    @DisplayName("Should retrieve draft order by ID")
    void testGetDraftOrderById() throws Exception {
        // Create draft order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = UUID.fromString(mapper.readTree(createResponse).get("id").asText());

        // Retrieve order
        mvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(ApiTestSupport.jwtWithScopes("order:read")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id", is(orderId.toString())),
                        jsonPath("$.status", is("DRAFT"))
                );
    }

    @Test
    @DisplayName("Should list orders for a customer")
    void testListOrdersForCustomer() throws Exception {
        // Create draft order
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        // List orders
        mvc.perform(get("/api/v1/orders?customerId={customerId}", ACTIVE_CUSTOMER_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:read")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$", hasSize(1)),
                        jsonPath("$[0].customerId", is(ACTIVE_CUSTOMER_ID))
                );
    }

    @Test
    @DisplayName("Should support idempotent order creation")
    void testIdempotentOrderCreation() throws Exception {
        String idempotencyKey = "key-idempotent-" + UUID.randomUUID();

        // First request
        String firstResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID firstOrderId = UUID.fromString(mapper.readTree(firstResponse).get("id").asText());

        // Replay with same key
        String secondResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID secondOrderId = UUID.fromString(mapper.readTree(secondResponse).get("id").asText());

        // Both should return the same order ID
        assert firstOrderId.equals(secondOrderId) : "Idempotent requests should return same order";
    }
}
