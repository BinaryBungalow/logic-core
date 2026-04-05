package ai.binbun.plugin.runtime;

import ai.binbun.plugin.manifest.PluginManifest;

public record PluginUpgradeOutcome(boolean success, String stage, PluginManifest activeManifest, PluginManifest rolledBackTo) {
}
