package ai.binbun.gateway.health;

import ai.binbun.plugin.runtime.PluginRuntimeManager;

public final class PluginHealthProbe implements HealthProbe {
    private final PluginRuntimeManager runtimeManager;

    public PluginHealthProbe(PluginRuntimeManager runtimeManager) {
        this.runtimeManager = runtimeManager;
    }

    @Override
    public String subsystemName() {
        return "plugins";
    }

    @Override
    public SubsystemHealth probe() {
        if (runtimeManager == null) {
            return new SubsystemHealth("plugins", "DOWN", "plugin runtime not initialized");
        }
        var records = runtimeManager.list();
        long activated = records.stream().filter(r -> r.state().name().equals("ACTIVATED")).count();
        long failed = records.stream().filter(r -> r.state().name().equals("FAILED")).count();
        if (failed > 0) {
            return new SubsystemHealth("plugins", "DEGRADED", failed + " plugin(s) failed, " + activated + " activated");
        }
        return new SubsystemHealth("plugins", activated > 0 ? "UP" : "DOWN", "activated=" + activated + "/" + records.size());
    }
}
