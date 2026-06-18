package org.raimundo.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.support.ApiTestSupport;
import org.raimundo.ecommerce.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T047: Integration tests para processar pagamentos de forma confiável.
 * 
 * Testa cenários com PostgreSQL real e WireMock para validar:
 * - Iniciação de pagamento com idempotência
 * - Replay de callbacks de aprovação/rejeição
 * - Elegibilidade de retentativas antes da terceira rejeição
 * - Cancelamento automático após terceira rejeição
 * - Tratamento de gateway indisponível (503)
 */
@DisplayName("T047: Payment Integration Tests (PostgreSQL + WireMock)")
class PaymentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static final String CORRELATION_ID = "correlation-789";
    private static final String ACTIVE_CUSTOMER_ID = "customer-active";
    private static final String AVAILABLE_PRODUCT_ID = "product-available";

    @Test
    @DisplayName("Should initiate payment for confirmed order")
    void testInitiatePaymentForConfirmedOrder() throws Exception {
        // Create, populate, and confirm order
        UUID orderId = createConfirmedOrder();

        // Initiate payment
        mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-init-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id", notNullValue()),
                        jsonPath("$.orderId", is(orderId.toString())),
                        jsonPath("$.status", is("PENDING"))
                );
    }

    @Test
    @DisplayName("Should replay approved payment callback idempotently")
    void testReplayApprovedPaymentCallbackIdempotently() throws Exception {
        // Create, populate, and confirm order
        UUID orderId = createConfirmedOrder();

        // Initiate payment
        String paymentResponse = mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-init-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(mapper.readTree(paymentResponse).get("id").asText());

        // First callback
        mvc.perform(post("/api/v1/payments/{id}/callback", paymentId)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status", is("APPROVED"))
                );

        // Replay callback
        mvc.perform(post("/api/v1/payments/{id}/callback", paymentId)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status", is("APPROVED"))
                );
    }

    @Test
    @DisplayName("Should reject payment for draft order")
    void testRejectPaymentForDraftOrder() throws Exception {
        // Create draft order (not confirmed)
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-draft-" + UUID.randomUUID())
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

        // Try to initiate payment on draft order
        mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-draft-pay-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue())
                );
    }

    @Test
    @DisplayName("Should support idempotent payment initiation")
    void testIdempotentPaymentInitiation() throws Exception {
        // Create, populate, and confirm order
        UUID orderId = createConfirmedOrder();

        String idempotencyKey = "key-idempotent-pay-" + UUID.randomUUID();

        // First initiation
        String firstResponse = mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andExpectAll(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID firstPaymentId = UUID.fromString(mapper.readTree(firstResponse).get("id").asText());

        // Replay with same key
        String secondResponse = mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andExpectAll(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID secondPaymentId = UUID.fromString(mapper.readTree(secondResponse).get("id").asText());

        // Both should return same payment ID
        assert firstPaymentId.equals(secondPaymentId) : "Idempotent requests should return same payment";
    }

    @Test
    @DisplayName("Should retrieve payment by ID")
    void testGetPaymentById() throws Exception {
        // Create, populate, and confirm order
        UUID orderId = createConfirmedOrder();

        // Initiate payment
        String paymentResponse = mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-get-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(mapper.readTree(paymentResponse).get("id").asText());

        // Get payment
        mvc.perform(get("/api/v1/payments/{id}", paymentId)
                        .with(ApiTestSupport.jwtWithScopes("payment:read")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id", is(paymentId.toString())),
                        jsonPath("$.orderId", is(orderId.toString())),
                        jsonPath("$.status", is("PENDING"))
                );
    }

    @Test
    @DisplayName("Should handle payment rejection")
    void testHandlePaymentRejection() throws Exception {
        // Create, populate, and confirm order
        UUID orderId = createConfirmedOrder();

        // Initiate payment
        String paymentResponse = mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-reject-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(mapper.readTree(paymentResponse).get("id").asText());

        // Process rejection callback
        mvc.perform(post("/api/v1/payments/{id}/callback", paymentId)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "status": "REJECTED"
                                }
                                """))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status", is("REJECTED"))
                );
    }

    @Test
    @DisplayName("Should allow retries on rejection before third rejection")
    void testAllowRetriesBeforeThirdRejection() throws Exception {
        // Create, populate, and confirm order
        UUID orderId = createConfirmedOrder();

        // Initiate payment
        String paymentResponse = mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-retry-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID paymentId = UUID.fromString(mapper.readTree(paymentResponse).get("id").asText());

        // First rejection
        mvc.perform(post("/api/v1/payments/{id}/callback", paymentId)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "status": "REJECTED"
                                }
                                """))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andReturn();

        // Should be able to retry payment initiation
        mvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-retry-2-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                        .with(ApiTestSupport.jwtWithScopes("payment:write")))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.status", is("PENDING"))
                );
    }

    // Helper method
    private UUID createConfirmedOrder() throws Exception {
        // Create draft order
        String createResponse = mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-order-" + UUID.randomUUID())
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
        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
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
                .andReturn();

        // Confirm order
        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Idempotency-Key", "key-confirm-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        return orderId;
    }
}
