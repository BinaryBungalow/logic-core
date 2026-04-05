package ai.binbun.gateway.health;

import ai.binbun.gateway.recovery.GatewayRecoveryCoordinator;

public final class GatewayOperationalSnapshotService {
    private final GatewayHealthService healthService;
    private final GatewayRecoveryCoordinator recoveryCoordinator;

    public GatewayOperationalSnapshotService(GatewayHealthService healthService, GatewayRecoveryCoordinator recoveryCoordinator) {
        this.healthService = healthService;
        this.recoveryCoordinator = recoveryCoordinator;
    }

    public GatewayOperationalSnapshot snapshot(boolean browserToolsReady, boolean deliveryReady, boolean pluginRuntimeReady) {
        return new GatewayOperationalSnapshot(
                healthService.snapshot(),
                recoveryCoordinator.startupPlan().size(),
                browserToolsReady,
                deliveryReady,
                pluginRuntimeReady
        );
    }
}
