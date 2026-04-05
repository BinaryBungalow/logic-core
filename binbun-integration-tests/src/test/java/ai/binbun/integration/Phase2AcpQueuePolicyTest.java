package ai.binbun.integration;

import ai.binbun.acp.socket.AcpOutboundQueueGuard;
import ai.binbun.acp.socket.AcpOutboundQueuePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2AcpQueuePolicyTest {
    @Test
    void slowConsumerTriggersDisconnectOrThrottle() {
        var disconnecting = new AcpOutboundQueueGuard(new AcpOutboundQueuePolicy(8, true)).evaluate(8);
        var throttled = new AcpOutboundQueueGuard(new AcpOutboundQueuePolicy(8, false)).evaluate(8);
        assertTrue(disconnecting.disconnect());
        assertFalse(disconnecting.accepted());
        assertTrue(throttled.throttled());
        assertFalse(throttled.accepted());
    }
}
