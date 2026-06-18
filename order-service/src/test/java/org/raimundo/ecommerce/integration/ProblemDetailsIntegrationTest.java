package org.raimundo.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.raimundo.ecommerce.support.ApiTestSupport;
import org.raimundo.ecommerce.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T058: Integration tests para RFC 7807 Problem Details.
 * 
 * Testa cenários com PostgreSQL real para validar:
 * - Erros de validação retornam Problem Details
 * - Erros de negócio retornam Problem Details
 * - Erros de idempotência retornam Problem Details
 * - Erros de autorização retornam Problem Details
 * - Erros de integração retornam Problem Details
 */
@DisplayName("T058: Problem Details Integration Tests (RFC 7807)")
class ProblemDetailsIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static final String CORRELATION_ID = "correlation-problem";
    private static final String ACTIVE_CUSTOMER_ID = "customer-active";
    private static final String AVAILABLE_PRODUCT_ID = "product-available";

    @Test
    @DisplayName("Should return Problem Details for validation error")
    void testValidationErrorReturnsProblemDetails() throws Exception {
        // Try to create order without customerId
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": ""
                                }
                                """))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue()),
                        jsonPath("$.title", notNullValue()),
                        jsonPath("$.status", notNullValue()),
                        jsonPath("$.detail", notNullValue())
                );
    }

    @Test
    @DisplayName("Should return Problem Details for business logic error")
    void testBusinessLogicErrorReturnsProblemDetails() throws Exception {
        // Try to create order for blocked customer
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "customer-blocked"
                                }
                                """))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue()),
                        jsonPath("$.title", notNullValue()),
                        jsonPath("$.status", notNullValue()),
                        jsonPath("$.detail", notNullValue())
                );
    }

    @Test
    @DisplayName("Should return Problem Details for invalid quantity")
    void testInvalidQuantityReturnsProblemDetails() throws Exception {
        // Create order
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

        // Try to add item with invalid quantity
        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Idempotency-Key", "key-invalid-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "productId": "%s",
                                  "quantity": -1
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue()),
                        jsonPath("$.title", notNullValue()),
                        jsonPath("$.status", notNullValue()),
                        jsonPath("$.detail", notNullValue())
                );
    }

    @Test
    @DisplayName("Should return Problem Details for missing Idempotency-Key")
    void testMissingIdempotencyKeyReturnsProblemDetails() throws Exception {
        // Try to add item without Idempotency-Key
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

        mvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "productId": "%s",
                                  "quantity": 1
                                }
                                """.formatted(AVAILABLE_PRODUCT_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue())
                );
    }

    @Test
    @DisplayName("Should return Problem Details for authorization error")
    void testAuthorizationErrorReturnsProblemDetails() throws Exception {
        // Try to create order without proper scope
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:read")))
                .andExpectAll(
                        status().isForbidden(),
                        jsonPath("$.type", notNullValue()),
                        jsonPath("$.title", notNullValue()),
                        jsonPath("$.status", notNullValue())
                );
    }

    @Test
    @DisplayName("Should return Problem Details for not found resource")
    void testNotFoundResourceReturnsProblemDetails() throws Exception {
        // Try to get non-existent order
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-get-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "%s"
                                }
                                """.formatted(ACTIVE_CUSTOMER_ID)))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andReturn();

        UUID nonExistentId = UUID.randomUUID();

        mvc.perform(post("/api/v1/orders/{id}/confirm", nonExistentId)
                        .header("Idempotency-Key", "key-not-found-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.type", notNullValue()),
                        jsonPath("$.title", notNullValue()),
                        jsonPath("$.status", notNullValue()),
                        jsonPath("$.detail", notNullValue())
                );
    }

    @Test
    @DisplayName("Should return Problem Details for external integration error")
    void testExternalIntegrationErrorReturnsProblemDetails() throws Exception {
        // Try to create order for non-existent customer
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .header("Correlation-Id", CORRELATION_ID)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "non-existent-customer"
                                }
                                """))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue()),
                        jsonPath("$.title", notNullValue()),
                        jsonPath("$.status", notNullValue())
                );
    }

    @Test
    @DisplayName("Should include correlation ID in Problem Details")
    void testProblemDetailsIncludesCorrelationId() throws Exception {
        String correlationIdValue = "corr-" + UUID.randomUUID();

        // Try to create order for blocked customer
        mvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .header("Correlation-Id", correlationIdValue)
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString("""
                                {
                                  "customerId": "customer-blocked"
                                }
                                """))
                        .with(ApiTestSupport.jwtWithScopes("order:write")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.type", notNullValue()),
                        jsonPath("$.correlationId", notNullValue())
                );
    }
}
