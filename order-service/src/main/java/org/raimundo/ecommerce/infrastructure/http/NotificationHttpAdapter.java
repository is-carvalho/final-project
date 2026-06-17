package org.raimundo.ecommerce.infrastructure.http;

import org.raimundo.ecommerce.application.ports.NotificationPort;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationHttpAdapter implements NotificationPort {
    private final WebClient client;

    public NotificationHttpAdapter(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.client = builder.baseUrl(properties.notificationBaseUrl()).build();
    }

    @Override
    public void publish(String eventType, UUID orderId, String customerId, String correlationId) {
        client.post()
                .uri("/notifications")
                .header("Correlation-Id", correlationId)
                .bodyValue(Map.of("eventId", UUID.randomUUID().toString(), "eventType", eventType,
                        "orderId", orderId.toString(), "customerId", customerId, "occurredAt", Instant.now().toString()))
                .retrieve()
                .toBodilessEntity()
                .onErrorComplete()
                .block();
    }
}
