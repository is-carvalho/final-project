package org.raimundo.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.support.ApiTestSupport;
import org.raimundo.ecommerce.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T057: Integration tests para segurança baseada em escopos.
 * 
 * Testa cenários com PostgreSQL real para validar:
 * - Endpoints restritos exigem escopos apropriados
 * - Operações de leitura requerem "order:read" e "payment:read"
 * - Operações de escrita requerem "order:write" e "payment:write"
 * - Requisições sem JWT são rejeitadas
 * - Requisições com escopos insuficientes são rejeitadas
 */
@DisplayName("T057: Security Integration Tests (Scopes)")
class SecurityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static final String CORRELATION_ID = "correlation-sec";
    private static final String ACTIVE_CUSTOMER_ID = "customer-active";
    private static final String AVAILABLE_PRODUCT_ID = "product-available";

    @Test
    @DisplayName("Should reject order creation without JWT")
    void testRejectOrderCreationWithoutJwt() throws Exception {
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject order creation without order:write scope")
    void testRejectOrderCreationWithoutOrderWriteScope() throws Exception {
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject order read without order:read scope")
    void testRejectOrderReadWithoutOrderReadScope() throws Exception {
        // Create order with proper scope
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = UUID.fromString(mapper.readTree(createResponse).get("id").asText());

        // Try to read without scope
        mvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("payment:read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow order read with order:read scope")
    void testAllowOrderReadWithOrderReadScope() throws Exception {
        // Create order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = UUID.fromString(mapper.readTree(createResponse).get("id").asText());

        // Read with proper scope
        mvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:read")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject add item without order:write scope")
    void testRejectAddItemWithoutOrderWriteScope() throws Exception {
        // Create order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = UUID.fromString(mapper.readTree(createResponse).get("id").asText());

        // Try to add item without scope
        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-item-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject confirm order without order:write scope")
    void testRejectConfirmOrderWithoutOrderWriteScope() throws Exception {
        // Create and populate order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-create-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = UUID.fromString(mapper.readTree(createResponse).get("id").asText());

        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-item-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        // Try to confirm without scope
        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-confirm-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject payment initiation without payment:write scope")
    void testRejectPaymentInitiationWithoutPaymentWriteScope() throws Exception {
        // Create confirmed order (with order:write scope)
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-order-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = UUID.fromString(mapper.readTree(createResponse).get("id").asText());

        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-item-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-confirm-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        // Try to initiate payment without payment:write scope
        mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-pay-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId))
                        .with(ApiTestSupport.jwtWithScopes("payment:read")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject payment read without payment:read scope")
    void testRejectPaymentReadWithoutPaymentReadScope() throws Exception {
        // Create confirmed order and initiate payment
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-order-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID orderId = UUID.fromString(mapper.readTree(createResponse).get("id").asText());

        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-item-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-confirm-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        String paymentResponse = mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-pay-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(mapper.readTree(paymentResponse).get("id").asText());

        // Try to read payment without scope
        mvc.perform(get("/api/v1/payments/{id}", paymentId)
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:read")))
                .andExpect(status().isForbidden());
    }
}
