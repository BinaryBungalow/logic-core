package ai.binbun.acp.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AcpEnvelopeCodec {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String encode(AcpEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode ACP envelope", e);
        }
    }

    public AcpEnvelope decode(String json) {
        try {
            return objectMapper.readValue(json, AcpEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to decode ACP envelope", e);
        }
    }
}
