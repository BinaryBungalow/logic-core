package ai.binbun.plugin.manifest;

import java.util.Map;

public final class PluginRuntimeStatusService {
    private final PluginEnvironmentReportService reports = new PluginEnvironmentReportService();

    public PluginRuntimeStatus snapshot(PluginManifest manifest, Map<String, String> environment) {
        var report = reports.evaluate(manifest, environment);
        return new PluginRuntimeStatus(manifest.name(), report.ready(), report.missingRequiredEnv().size());
    }
}
