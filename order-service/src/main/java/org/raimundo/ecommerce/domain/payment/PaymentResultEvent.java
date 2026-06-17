package org.raimundo.ecommerce.domain.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentResultEvent(UUID id, UUID paymentTransactionId, String providerEventId, String eventType,
                                 Instant receivedAt, String correlationId, String payloadHash) {
    public static PaymentResultEvent received(UUID paymentTransactionId, String providerEventId, String eventType,
                                              String correlationId, String payloadHash) {
        return new PaymentResultEvent(UUID.randomUUID(), paymentTransactionId, providerEventId, eventType,
                Instant.now(), correlationId, payloadHash);
    }
}
