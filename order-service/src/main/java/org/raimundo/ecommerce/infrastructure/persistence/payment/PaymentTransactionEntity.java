package org.raimundo.ecommerce.infrastructure.persistence.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.raimundo.ecommerce.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransactionEntity {
    @Id
    public UUID id;
    public UUID orderId;
    @Enumerated(EnumType.STRING)
    public PaymentStatus status;
    public int attemptNumber;
    public String idempotencyKey;
    public String providerTransactionId;
    public BigDecimal amount;
    public String currency;
    public Instant requestedAt;
    public Instant processedAt;
    public String failureReason;
    public String correlationId;
    @Version
    public long version;
}
