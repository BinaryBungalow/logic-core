package ai.binbun.plugin.manifest;

import java.util.List;

public record PluginEnvironmentReport(boolean ready, List<String> missingRequiredEnv) {
    public PluginEnvironmentReport {
        missingRequiredEnv = missingRequiredEnv == null ? List.of() : List.copyOf(missingRequiredEnv);
    }
}
