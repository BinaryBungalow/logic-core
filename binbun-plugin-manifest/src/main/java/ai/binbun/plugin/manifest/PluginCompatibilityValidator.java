package ai.binbun.plugin.manifest;

public final class PluginCompatibilityValidator {
    public boolean isCompatible(PluginManifest manifest, String runtimeCompatibility) {
        if (manifest.compatibility() == null || manifest.compatibility().isBlank()) {
            return true;
        }
        return manifest.compatibility().equals(runtimeCompatibility);
    }
}
