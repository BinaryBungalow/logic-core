package ai.binbun.gateway.health;

import ai.binbun.delivery.core.ConnectorRegistry;

public final class DeliveryHealthProbe implements HealthProbe {
    private final ConnectorRegistry connectorRegistry;

    public DeliveryHealthProbe(ConnectorRegistry connectorRegistry) {
        this.connectorRegistry = connectorRegistry;
    }

    @Override
    public String subsystemName() {
        return "delivery";
    }

    @Override
    public SubsystemHealth probe() {
        if (connectorRegistry == null || connectorRegistry.all().isEmpty()) {
            return new SubsystemHealth("delivery", "DOWN", "no connectors registered");
        }
        var connectors = connectorRegistry.all();
        long degraded = connectors.stream().filter(c -> c.health().status().equals("DEGRADED")).count();
        long down = connectors.stream().filter(c -> c.health().status().equals("DOWN")).count();
        if (down > 0) {
            return new SubsystemHealth("delivery", "DOWN", down + " connector(s) down");
        }
        if (degraded > 0) {
            return new SubsystemHealth("delivery", "DEGRADED", degraded + " connector(s) degraded");
        }
        return new SubsystemHealth("delivery", "UP", "connectors=" + connectors.size());
    }
}
