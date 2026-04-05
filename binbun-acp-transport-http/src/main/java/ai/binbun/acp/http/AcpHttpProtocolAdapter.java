package ai.binbun.acp.http;

import ai.binbun.acp.protocol.AcpEnvelope;
import ai.binbun.acp.protocol.AcpEnvelopeCodec;

public final class AcpHttpProtocolAdapter {
    private final AcpEnvelopeCodec codec = new AcpEnvelopeCodec();

    public String encode(AcpEnvelope envelope) {
        return codec.encode(envelope);
    }

    public AcpEnvelope decode(String payload) {
        return codec.decode(payload);
    }
}
