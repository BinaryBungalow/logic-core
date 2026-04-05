package ai.binbun.acp.socket;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcpSocketConnectionStateTest {
    @Test
    void tracksDuplicateSubscriptionPerConnection() {
        var state = new AcpSocketConnectionState();
        assertTrue(state.markSubscribed("s1"));
        assertFalse(state.markSubscribed("s1"));
        assertTrue(state.markSubscribed("s2"));
    }

    @Test
    void heartbeatExpirationDependsOnRecentActivity() throws Exception {
        var state = new AcpSocketConnectionState();
        assertFalse(state.heartbeatExpired(Duration.ofSeconds(1)));
        Thread.sleep(5L);
        assertTrue(state.heartbeatExpired(Duration.ZERO));
    }
}
