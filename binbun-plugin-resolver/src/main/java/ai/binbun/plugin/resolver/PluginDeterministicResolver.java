package ai.binbun.plugin.resolver;

import ai.binbun.plugin.manifest.PluginManifest;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PluginDeterministicResolver {
    public PluginLockfile resolveLockfile(List<PluginManifest> manifests) {
        Map<String, String> selected = new LinkedHashMap<>();
        for (PluginManifest manifest : manifests.stream().sorted(Comparator.comparing(PluginManifest::name).thenComparing(PluginManifest::version)).toList()) {
            String existing = selected.get(manifest.name());
            if (existing != null && !existing.equals(manifest.version())) {
                throw new IllegalArgumentException("Dependency conflict for plugin: " + manifest.name());
            }
            selected.put(manifest.name(), manifest.version());
        }
        return new PluginLockfile(selected.entrySet().stream().map(entry -> new PluginLockfileEntry(entry.getKey(), entry.getValue())).toList());
    }
}
