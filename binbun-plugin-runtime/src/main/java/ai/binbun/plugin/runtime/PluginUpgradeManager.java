package ai.binbun.plugin.runtime;

import ai.binbun.plugin.manifest.PluginManifest;

public final class PluginUpgradeManager {
    public PluginUpgradeOutcome upgrade(PluginManifest previous, PluginManifest candidate,
                                        boolean compatibilityOk,
                                        boolean permissionsOk,
                                        boolean migrationsOk,
                                        boolean activationOk) {
        if (!compatibilityOk) {
            return new PluginUpgradeOutcome(false, "compatibility", previous, previous);
        }
        if (!permissionsOk) {
            return new PluginUpgradeOutcome(false, "permissions", previous, previous);
        }
        if (!migrationsOk) {
            return new PluginUpgradeOutcome(false, "migrations", previous, previous);
        }
        if (!activationOk) {
            return new PluginUpgradeOutcome(false, "activation", previous, previous);
        }
        return new PluginUpgradeOutcome(true, "complete", candidate, null);
    }
}
