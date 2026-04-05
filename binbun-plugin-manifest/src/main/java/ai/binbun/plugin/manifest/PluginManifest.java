package ai.binbun.plugin.manifest;

import java.util.List;

public record PluginManifest(
        String name,
        String version,
        String entrypoint,
        List<PluginDependency> dependencies,
        List<PluginDependency> optionalDependencies,
        List<String> tools,
        List<String> workflows,
        List<PluginPermission> permissions,
        List<String> requiredEnv,
        List<String> migrations,
        String compatibility
) {
    public PluginManifest {
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        optionalDependencies = optionalDependencies == null ? List.of() : List.copyOf(optionalDependencies);
        tools = tools == null ? List.of() : List.copyOf(tools);
        workflows = workflows == null ? List.of() : List.copyOf(workflows);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
        requiredEnv = requiredEnv == null ? List.of() : List.copyOf(requiredEnv);
        migrations = migrations == null ? List.of() : List.copyOf(migrations);
    }
}
