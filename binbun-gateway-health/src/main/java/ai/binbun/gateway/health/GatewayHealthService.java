package ai.binbun.gateway.health;

import java.util.List;

public final class GatewayHealthService {
    private final List<HealthProbe> probes;

    public GatewayHealthService(List<HealthProbe> probes) {
        this.probes = probes == null ? List.of() : List.copyOf(probes);
    }

    public GatewayHealthService() {
        this(List.of());
    }

    public GatewayHealthReport snapshot() {
        List<SubsystemHealth> subsystemHealths = probes.stream()
                .map(HealthProbe::probe)
                .toList();

        String liveness = "UP";
        long downCount = subsystemHealths.stream().filter(h -> "DOWN".equals(h.status())).count();
        long degradedCount = subsystemHealths.stream().filter(h -> "DEGRADED".equals(h.status())).count();

        String readiness = downCount > 0 ? "DOWN" : (degradedCount > 0 ? "DEGRADED" : "UP");
        String degraded = degradedCount > 0 ? "DEGRADED" : "NONE";

        return new GatewayHealthReport(liveness, readiness, degraded, subsystemHealths);
    }
}
