package ai.binbun.gateway.health;

import java.util.List;

public final class GatewayReadinessSnapshotService {
    public GatewayReadinessSnapshot snapshot(List<GatewaySubsystemStatus> subsystems) {
        boolean degraded = subsystems.stream().anyMatch(status -> "DEGRADED".equals(status.state()) || "DOWN".equals(status.state()));
        boolean down = subsystems.stream().anyMatch(status -> "DOWN".equals(status.state()));
        return new GatewayReadinessSnapshot(
                "UP",
                down ? "DOWN" : "UP",
                degraded,
                subsystems
        );
    }
}
