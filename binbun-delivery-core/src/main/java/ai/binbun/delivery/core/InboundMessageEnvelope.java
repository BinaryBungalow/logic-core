package ai.binbun.delivery.core;

import java.time.Instant;
import java.util.Map;

public record InboundMessageEnvelope(
        String connector,
        String source,
        String text,
        String providerMessageId,
        Map<String, Object> metadata,
        Instant receivedAt
) {
    public InboundMessageEnvelope {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }
}
