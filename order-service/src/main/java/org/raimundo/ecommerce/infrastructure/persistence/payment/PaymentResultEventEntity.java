package org.raimundo.ecommerce.infrastructure.persistence.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_result_events")
public class PaymentResultEventEntity {
    @Id
    public UUID id;
    public UUID paymentTransactionId;
    public String providerEventId;
    public String eventType;
    public Instant receivedAt;
    public String correlationId;
    public String payloadHash;
}
