package ai.binbun.plugin.manifest;

import java.util.List;
import java.util.Map;

public final class PluginEnvironmentValidator {
    public List<String> missingRequiredEnv(PluginManifest manifest, Map<String, String> environment) {
        return manifest.requiredEnv().stream()
                .filter(name -> !environment.containsKey(name) || environment.get(name) == null || environment.get(name).isBlank())
                .toList();
    }
}
