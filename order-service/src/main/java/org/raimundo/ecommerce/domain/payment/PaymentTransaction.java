package org.raimundo.ecommerce.domain.payment;

import org.raimundo.ecommerce.domain.common.Money;

import java.time.Instant;
import java.util.UUID;

public class PaymentTransaction {
    private final UUID id;
    private final UUID orderId;
    private PaymentStatus status;
    private final int attemptNumber;
    private final String idempotencyKey;
    private String providerTransactionId;
    private final Money amount;
    private String failureReason;
    private final Instant requestedAt;
    private Instant processedAt;

    public PaymentTransaction(UUID id, UUID orderId, PaymentStatus status, int attemptNumber, String idempotencyKey,
                              String providerTransactionId, Money amount, String failureReason,
                              Instant requestedAt, Instant processedAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.orderId = orderId;
        this.status = status == null ? PaymentStatus.PENDING : status;
        this.attemptNumber = attemptNumber;
        this.idempotencyKey = idempotencyKey;
        this.providerTransactionId = providerTransactionId;
        this.amount = amount;
        this.failureReason = failureReason;
        this.requestedAt = requestedAt == null ? Instant.now() : requestedAt;
        this.processedAt = processedAt;
    }

    public static PaymentTransaction pending(UUID orderId, int attemptNumber, String idempotencyKey, Money amount) {
        return new PaymentTransaction(UUID.randomUUID(), orderId, PaymentStatus.PENDING, attemptNumber, idempotencyKey, null, amount, null, Instant.now(), null);
    }

    public void approve(String providerTransactionId) {
        status = PaymentStatus.APPROVED;
        this.providerTransactionId = providerTransactionId;
        processedAt = Instant.now();
    }

    public void reject(String providerTransactionId, String reason) {
        status = PaymentStatus.REJECTED;
        this.providerTransactionId = providerTransactionId;
        failureReason = reason;
        processedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID orderId() { return orderId; }
    public PaymentStatus status() { return status; }
    public int attemptNumber() { return attemptNumber; }
    public String idempotencyKey() { return idempotencyKey; }
    public String providerTransactionId() { return providerTransactionId; }
    public Money amount() { return amount; }
    public String failureReason() { return failureReason; }
    public Instant requestedAt() { return requestedAt; }
    public Instant processedAt() { return processedAt; }
}
