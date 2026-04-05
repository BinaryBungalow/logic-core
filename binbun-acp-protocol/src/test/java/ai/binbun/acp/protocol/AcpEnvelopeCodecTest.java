package ai.binbun.acp.protocol;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcpEnvelopeCodecTest {
    @Test
    void roundTripsEnvelope() {
        var codec = new AcpEnvelopeCodec();
        var envelope = AcpEnvelopes.request(AcpOperation.HELLO, "s1", 1L, "c1", Map.of("agent", "binbun-java"));
        var decoded = codec.decode(codec.encode(envelope));
        assertEquals(AcpProtocolVersion.V1ALPHA1, decoded.protocolVersion());
        assertEquals(AcpOperation.HELLO, decoded.op());
        assertEquals("s1", decoded.sessionId());
        assertEquals("binbun-java", decoded.payload().get("agent"));
    }
}
