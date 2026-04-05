package ai.binbun.acp.socket;

import ai.binbun.acp.protocol.AcpEnvelopes;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcpReplayBufferTest {
    @Test
    void keepsOnlyConfiguredCapacityAndReplaysFromSequence() {
        var buffer = new AcpReplayBuffer(2);
        buffer.add(AcpEnvelopes.event("s1", 1L, null, Map.of("eventType", "a")));
        buffer.add(AcpEnvelopes.event("s1", 2L, null, Map.of("eventType", "b")));
        buffer.add(AcpEnvelopes.event("s1", 3L, null, Map.of("eventType", "c")));
        var replay = buffer.since(1L);
        assertEquals(2, replay.size());
        assertEquals(2L, replay.get(0).sequence());
        assertEquals(3L, replay.get(1).sequence());
    }
}
