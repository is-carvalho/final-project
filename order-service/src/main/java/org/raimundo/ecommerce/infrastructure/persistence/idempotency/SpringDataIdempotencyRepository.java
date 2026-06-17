package org.raimundo.ecommerce.infrastructure.persistence.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataIdempotencyRepository extends JpaRepository<IdempotencyEntity, UUID> {
    Optional<IdempotencyEntity> findByOperationAndResourceIdAndCallerIdAndIdempotencyKey(String operation, String resourceId,
                                                                                         String callerId, String idempotencyKey);
}
