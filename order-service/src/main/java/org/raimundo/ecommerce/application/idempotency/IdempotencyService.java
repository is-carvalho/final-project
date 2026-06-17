package org.raimundo.ecommerce.application.idempotency;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {
    private final IdempotencyRepositoryPort records;

    public IdempotencyService(IdempotencyRepositoryPort records) {
        this.records = records;
    }

    public Optional<String> replay(String operation, String resourceId, String callerId, String key, String requestBody) {
        requireKey(key);
        String hash = hash(requestBody == null ? "" : requestBody);
        return records.find(operation, resourceId, callerId, key).flatMap(record -> record.replayBodyFor(hash));
    }

    public void remember(String operation, String resourceId, String callerId, String key, String requestBody,
                         int responseStatus, String responseBody) {
        requireKey(key);
        records.save(new IdempotencyRecord(UUID.randomUUID(), operation, resourceId, callerId, key,
                hash(requestBody == null ? "" : requestBody), responseStatus, responseBody,
                Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS)));
    }

    public static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IdempotencyConflictException("Idempotency-Key header is required");
        }
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
