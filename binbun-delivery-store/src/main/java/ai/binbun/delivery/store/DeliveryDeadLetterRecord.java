package ai.binbun.delivery.store;

import java.time.Instant;

public record DeliveryDeadLetterRecord(
        String connector,
        String idempotencyKey,
        String sessionId,
        String reason,
        Instant createdAt
) {
    public DeliveryDeadLetterRecord {
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
