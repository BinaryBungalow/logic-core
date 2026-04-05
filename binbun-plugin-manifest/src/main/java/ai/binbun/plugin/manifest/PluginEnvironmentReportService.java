package ai.binbun.plugin.manifest;

import java.util.Map;

public final class PluginEnvironmentReportService {
    private final PluginEnvironmentValidator validator = new PluginEnvironmentValidator();

    public PluginEnvironmentReport evaluate(PluginManifest manifest, Map<String, String> environment) {
        var missing = validator.missingRequiredEnv(manifest, environment);
        return new PluginEnvironmentReport(missing.isEmpty(), missing);
    }
}
