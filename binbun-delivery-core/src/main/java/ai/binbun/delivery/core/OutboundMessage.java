package ai.binbun.delivery.core;

import java.time.Instant;
import java.util.Map;

public record OutboundMessage(
        String idempotencyKey,
        String sessionId,
        String destination,
        String text,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public OutboundMessage {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
