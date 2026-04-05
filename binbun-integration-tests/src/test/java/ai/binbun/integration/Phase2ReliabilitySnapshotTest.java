package ai.binbun.integration;

import ai.binbun.gateway.health.GatewayReadinessSnapshotService;
import ai.binbun.gateway.health.GatewaySubsystemStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2ReliabilitySnapshotTest {
    @Test
    void degradedSubsystemsDoNotFailLiveness() {
        var snapshot = new GatewayReadinessSnapshotService().snapshot(List.of(
                new GatewaySubsystemStatus("acp", "UP", "healthy"),
                new GatewaySubsystemStatus("delivery", "DEGRADED", "connector outage"),
                new GatewaySubsystemStatus("plugins", "UP", "ready")
        ));
        assertEquals("UP", snapshot.liveness());
        assertEquals("UP", snapshot.readiness());
        assertTrue(snapshot.degraded());
    }
}
