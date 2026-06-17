package org.raimundo.ecommerce.application.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record IdempotencyRecord(UUID id, String operation, String resourceId, String callerId, String idempotencyKey,
                                String requestHash, int responseStatus, String responseBody, Instant createdAt,
                                Instant expiresAt) {
    public Optional<String> replayBodyFor(String hash) {
        if (!requestHash.equals(hash)) {
            throw new IdempotencyConflictException("Idempotency key was reused with a different request");
        }
        return Optional.ofNullable(responseBody);
    }
}
