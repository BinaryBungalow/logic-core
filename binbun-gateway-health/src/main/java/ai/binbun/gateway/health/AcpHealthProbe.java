package ai.binbun.gateway.health;

import ai.binbun.acp.AcpServer;

public final class AcpHealthProbe implements HealthProbe {
    private final AcpServer acpServer;

    public AcpHealthProbe(AcpServer acpServer) {
        this.acpServer = acpServer;
    }

    @Override
    public String subsystemName() {
        return "acp";
    }

    @Override
    public SubsystemHealth probe() {
        if (acpServer == null) {
            return new SubsystemHealth("acp", "DOWN", "acp server not initialized");
        }
        int sessions = acpServer.size();
        return new SubsystemHealth("acp", "UP", "sessions=" + sessions);
    }
}
