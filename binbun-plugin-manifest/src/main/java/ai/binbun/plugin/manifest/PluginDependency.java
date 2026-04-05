package ai.binbun.plugin.manifest;

public record PluginDependency(String name, String versionRange, boolean optional) {
}
