package ai.binbun.gateway.recovery;

import ai.binbun.plugin.registry.PluginRegistryService;
import ai.binbun.plugin.runtime.PluginRuntimeManager;

public final class PluginRecoveryExecutor implements RecoveryExecutor {
    private final PluginRegistryService registryService;
    private final PluginRuntimeManager runtimeManager;

    public PluginRecoveryExecutor(PluginRegistryService registryService,
                                  PluginRuntimeManager runtimeManager) {
        this.registryService = registryService;
        this.runtimeManager = runtimeManager;
    }

    @Override
    public String subsystemName() {
        return "plugins";
    }

    @Override
    public RecoveryCheckpoint execute() {
        if (registryService == null || runtimeManager == null) {
            return new RecoveryCheckpoint("plugins", "SKIPPED", "plugin registry or runtime not available");
        }
        try {
            var bundled = registryService.listBundled();
            int activated = 0;
            for (var entry : bundled) {
                try {
                    runtimeManager.activate(entry.name());
                    activated++;
                } catch (Exception e) {
                    // Plugin activation failed, continue with others
                }
            }
            return new RecoveryCheckpoint("plugins", "RECOVERED", "reactivated " + activated + "/" + bundled.size() + " plugin(s)");
        } catch (Exception e) {
            return new RecoveryCheckpoint("plugins", "FAILED", "plugin recovery error: " + e.getMessage());
        }
    }
}
