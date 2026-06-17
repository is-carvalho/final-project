package org.raimundo.ecommerce.infrastructure.http;

import org.raimundo.ecommerce.application.ports.PaymentGatewayPort;
import org.raimundo.ecommerce.domain.common.Money;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Component
public class PaymentGatewayHttpAdapter implements PaymentGatewayPort {
    private final WebClient client;

    public PaymentGatewayHttpAdapter(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.client = builder.baseUrl(properties.paymentBaseUrl()).build();
    }

    @Override
    public GatewayResult initiate(UUID paymentId, UUID orderId, Money amount, String idempotencyKey, String correlationId) {
        return client.post()
                .uri("/payments")
                .header("Correlation-Id", correlationId)
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(Map.of("paymentId", paymentId, "orderId", orderId, "amount", amount.amount(), "currency", amount.currency()))
                .retrieve()
                .bodyToMono(GatewayResult.class)
                .onErrorReturn(new GatewayResult(null, "PENDING", "gateway temporarily unavailable"))
                .block();
    }
}
