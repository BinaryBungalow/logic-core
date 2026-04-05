package ai.binbun.acp.protocol;

import java.util.Map;
import java.util.UUID;

public final class AcpEnvelopes {
    private AcpEnvelopes() {
    }

    public static AcpEnvelope request(AcpOperation op, String sessionId, long sequence, String correlationId, Map<String, Object> payload) {
        return new AcpEnvelope(AcpProtocolVersion.V1ALPHA1, UUID.randomUUID().toString(), AcpMessageType.REQUEST, op,
                sessionId, sequence, correlationId, payload, null);
    }

    public static AcpEnvelope response(AcpOperation op, String sessionId, long sequence, String correlationId, Map<String, Object> payload) {
        return new AcpEnvelope(AcpProtocolVersion.V1ALPHA1, UUID.randomUUID().toString(), AcpMessageType.RESPONSE, op,
                sessionId, sequence, correlationId, payload, null);
    }

    public static AcpEnvelope event(String sessionId, long sequence, String correlationId, Map<String, Object> payload) {
        return new AcpEnvelope(AcpProtocolVersion.V1ALPHA1, UUID.randomUUID().toString(), AcpMessageType.EVENT, AcpOperation.EVENT,
                sessionId, sequence, correlationId, payload, null);
    }

    public static AcpEnvelope error(AcpOperation op, String sessionId, long sequence, String correlationId, String code, String message) {
        return new AcpEnvelope(AcpProtocolVersion.V1ALPHA1, UUID.randomUUID().toString(), AcpMessageType.ERROR, op,
                sessionId, sequence, correlationId, Map.of(), new AcpError(code, message));
    }
}
