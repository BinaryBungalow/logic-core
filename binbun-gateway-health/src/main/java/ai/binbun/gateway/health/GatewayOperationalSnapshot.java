package ai.binbun.gateway.health;

public record GatewayOperationalSnapshot(
        GatewayHealthReport health,
        int plannedRecoverySteps,
        boolean browserToolsReady,
        boolean deliveryReady,
        boolean pluginRuntimeReady
) {
}
