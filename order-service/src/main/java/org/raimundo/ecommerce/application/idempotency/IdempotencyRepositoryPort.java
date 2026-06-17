package org.raimundo.ecommerce.application.idempotency;

import java.util.Optional;

public interface IdempotencyRepositoryPort {
    Optional<IdempotencyRecord> find(String operation, String resourceId, String callerId, String idempotencyKey);
    void save(IdempotencyRecord record);
}
