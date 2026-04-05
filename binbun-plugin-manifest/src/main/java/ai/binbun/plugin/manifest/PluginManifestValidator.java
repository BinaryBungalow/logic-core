package ai.binbun.plugin.manifest;

public final class PluginManifestValidator {
    public void validate(PluginManifest manifest) {
        if (manifest.name() == null || manifest.name().isBlank()) {
            throw new IllegalArgumentException("Plugin manifest requires name");
        }
        if (manifest.version() == null || manifest.version() == null || manifest.version().isBlank()) {
            throw new IllegalArgumentException("Plugin manifest requires version");
        }
        if (manifest.entrypoint() == null || manifest.entrypoint().isBlank()) {
            throw new IllegalArgumentException("Plugin manifest requires entrypoint");
        }
    }
}
