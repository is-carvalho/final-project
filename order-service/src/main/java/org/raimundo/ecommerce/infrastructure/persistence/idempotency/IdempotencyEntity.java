package org.raimundo.ecommerce.infrastructure.persistence.idempotency;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyEntity {
    @Id
    public UUID id;
    public String operation;
    public String resourceId;
    public String callerId;
    public String idempotencyKey;
    public String requestHash;
    public int responseStatus;
    @Column(columnDefinition = "text")
    public String responseBody;
    public Instant createdAt;
    public Instant expiresAt;
}
