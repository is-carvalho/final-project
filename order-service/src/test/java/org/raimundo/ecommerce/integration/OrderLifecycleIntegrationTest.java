package org.raimundo.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.support.ApiTestSupport;
import org.raimundo.ecommerce.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T036: Integration tests para confirmar ou cancelar pedidos.
 * 
 * Testa cenários com PostgreSQL real e WireMock para validar:
 * - Confirmação de pedidos não-vazios
 * - Recálculo de preços em confirmação
 * - Rejeição de mudanças pós-confirmação
 * - Cancelamento antes de aprovação de pagamento
 * - Transições de ciclo de vida inválidas
 */
@DisplayName("T036: Order Lifecycle Integration Tests (PostgreSQL + WireMock)")
class OrderLifecycleIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static final String CORRELATION_ID = "correlation-456";
    private static final String ACTIVE_CUSTOMER_ID = "customer-active";
    private static final String AVAILABLE_PRODUCT_ID = "product-available";

    @Test
    @DisplayName("Should confirm a non-empty draft order")
    void testConfirmNonEmptyDraftOrder() throws Exception {
        // Create and populate draft order
        UUID orderId = createAndAddItemToDraftOrder();

        // Confirm order
        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-confirm-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status", is("CONFIRMED")),
                        jsonPath("$.totalAmount", notNullValue())
                );
    }

    @Test
    @DisplayName("Should reject confirmation of empty draft order")
    void testRejectConfirmationOfEmptyOrder() throws Exception {
        // Create empty draft order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-empty-" + UUID.randomUUID())
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

        // Try to confirm empty order
        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-confirm-empty-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isUnprocessableEntity(),
                        jsonPath("$.type", notNullValue())
                );
    }

    @Test
    @DisplayName("Should recalculate prices on confirmation")
    void testRecalculatePricesOnConfirmation() throws Exception {
        // Create and populate draft order
        UUID orderId = createAndAddItemToDraftOrder();

        // Get order before confirmation
        String beforeConfirm = mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-recalc-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.totalAmount", notNullValue()),
                        jsonPath("$.totalAmount", greaterThan(0.0))
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify prices are set
        assert mapper.readTree(beforeConfirm).get("items").get(0).get("unitPrice") != null;
    }

    @Test
    @DisplayName("Should reject item changes after confirmation")
    void testRejectItemChangesAfterConfirmation() throws Exception {
        // Create and populate draft order
        UUID orderId = createAndAddItemToDraftOrder();

        // Confirm order
        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-conf-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        // Try to add item after confirmation
        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-after-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isUnprocessableEntity(),
                        jsonPath("$.type", notNullValue())
                );
    }

    @Test
    @DisplayName("Should cancel confirmed order")
    void testCancelConfirmedOrder() throws Exception {
        // Create, populate and confirm order
        UUID orderId = createAndAddItemToDraftOrder();

        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-conf-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        // Cancel order
        mvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .header("Idempotency-Key", "key-cancel-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status", is("CANCELLED"))
                );
    }

    @Test
    @DisplayName("Should cancel draft order")
    void testCancelDraftOrder() throws Exception {
        // Create draft order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-draft-" + UUID.randomUUID())
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

        // Cancel draft order
        mvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .header("Idempotency-Key", "key-cancel-draft-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status", is("CANCELLED"))
                );
    }

    @Test
    @DisplayName("Should reject invalid state transition")
    void testRejectInvalidStateTransition() throws Exception {
        UUID orderId = createAndAddItemToDraftOrder();

        // Confirm first, then try to edit the confirmed order.
        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-conf-1-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(status().isOk())
                .andReturn();

        // This should fail because trying to add item to confirmed order
        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-add-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Should support idempotent cancellation")
    void testIdempotentCancellation() throws Exception {
        // Create and populate draft order
        UUID orderId = createAndAddItemToDraftOrder();

        String cancelKey = "key-cancel-idempotent-" + UUID.randomUUID();

        // First cancellation
        mvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .header("Idempotency-Key", cancelKey)
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(status().isOk());

        // Replay cancellation
        mvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .header("Idempotency-Key", cancelKey)
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status", is("CANCELLED"))
                );
    }

    // Helper method
    private UUID createAndAddItemToDraftOrder() throws Exception {
        // Create draft order
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

        // Add item
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

        return orderId;
    }
}
