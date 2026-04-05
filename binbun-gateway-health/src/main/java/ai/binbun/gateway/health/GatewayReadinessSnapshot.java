package ai.binbun.gateway.health;

import java.util.List;

public record GatewayReadinessSnapshot(
        String liveness,
        String readiness,
        boolean degraded,
        List<GatewaySubsystemStatus> subsystems
) {
    public GatewayReadinessSnapshot {
        subsystems = subsystems == null ? List.of() : List.copyOf(subsystems);
    }
}
