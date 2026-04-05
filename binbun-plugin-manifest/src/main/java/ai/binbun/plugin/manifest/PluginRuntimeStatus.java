package ai.binbun.plugin.manifest;

public record PluginRuntimeStatus(
        String pluginName,
        boolean ready,
        int missingRequiredEnvCount
) {
}
