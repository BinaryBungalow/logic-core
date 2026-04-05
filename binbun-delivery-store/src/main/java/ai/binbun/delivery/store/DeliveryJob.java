package ai.binbun.delivery.store;

import java.time.Instant;

public record DeliveryJob(
        String connector,
        String idempotencyKey,
        String sessionId,
        String destination,
        String payload,
        DeliveryJobStatus status,
        int retryCount,
        String lastError,
        String providerMessageId,
        Instant createdAt,
        Instant updatedAt
) {
    public DeliveryJob {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }
}
