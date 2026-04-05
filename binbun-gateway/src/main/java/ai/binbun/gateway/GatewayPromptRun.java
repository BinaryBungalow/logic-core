package ai.binbun.gateway;

import java.time.Instant;

public record GatewayPromptRun(String id, String sessionId, String input, Instant createdAt) {
    public GatewayPromptRun {
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
