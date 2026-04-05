package ai.binbun.gateway.health;

import java.util.List;

public record GatewayHealthReport(String liveness, String readiness, String degraded, List<SubsystemHealth> subsystems) {
    public GatewayHealthReport {
        subsystems = subsystems == null ? List.of() : List.copyOf(subsystems);
    }
}
