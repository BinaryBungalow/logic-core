package ai.binbun.acp.protocol;

import java.util.Map;

public record AcpEnvelope(
        String protocolVersion,
        String messageId,
        AcpMessageType type,
        AcpOperation op,
        String sessionId,
        long sequence,
        String correlationId,
        Map<String, Object> payload,
        AcpError error
) {
    public AcpEnvelope {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
