package org.raimundo.ecommerce.infrastructure.persistence.idempotency;

import org.raimundo.ecommerce.application.idempotency.IdempotencyRecord;
import org.raimundo.ecommerce.application.idempotency.IdempotencyRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaIdempotencyRepositoryAdapter implements IdempotencyRepositoryPort {
    private final SpringDataIdempotencyRepository repository;

    public JpaIdempotencyRepositoryAdapter(SpringDataIdempotencyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<IdempotencyRecord> find(String operation, String resourceId, String callerId, String idempotencyKey) {
        return repository.findByOperationAndResourceIdAndCallerIdAndIdempotencyKey(operation, resourceId, callerId, idempotencyKey)
                .map(entity -> new IdempotencyRecord(entity.id, entity.operation, entity.resourceId, entity.callerId,
                        entity.idempotencyKey, entity.requestHash, entity.responseStatus, entity.responseBody,
                        entity.createdAt, entity.expiresAt));
    }

    @Override
    public void save(IdempotencyRecord record) {
        IdempotencyEntity entity = new IdempotencyEntity();
        entity.id = record.id();
        entity.operation = record.operation();
        entity.resourceId = record.resourceId();
        entity.callerId = record.callerId();
        entity.idempotencyKey = record.idempotencyKey();
        entity.requestHash = record.requestHash();
        entity.responseStatus = record.responseStatus();
        entity.responseBody = record.responseBody();
        entity.createdAt = record.createdAt();
        entity.expiresAt = record.expiresAt();
        repository.save(entity);
    }
}
