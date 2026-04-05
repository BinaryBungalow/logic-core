package ai.binbun.acp.socket;

import ai.binbun.acp.protocol.AcpEnvelopes;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcpOutboundQueueTest {
    @Test
    void rejectsOfferWhenCapacityIsExceeded() {
        var queue = new AcpOutboundQueue(1);
        assertTrue(queue.offer(AcpEnvelopes.event("s1", 1L, null, Map.of("eventType", "a"))));
        assertFalse(queue.offer(AcpEnvelopes.event("s1", 2L, null, Map.of("eventType", "b"))));
        assertEquals(1, queue.drain().size());
    }
}
