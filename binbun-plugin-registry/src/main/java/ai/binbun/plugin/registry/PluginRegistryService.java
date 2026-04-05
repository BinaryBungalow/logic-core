package ai.binbun.plugin.registry;

import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.manifest.PluginManifestLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PluginRegistryService {
    private final List<PluginRegistryEntry> bundled;
    private final List<PluginRegistryEntry> discovered;

    public PluginRegistryService(List<PluginRegistryEntry> bundled) {
        this.bundled = bundled == null ? List.of() : List.copyOf(bundled);
        this.discovered = new ArrayList<>();
    }

    public PluginRegistryService() {
        this(List.of(new PluginRegistryEntry("core-plugin", "0.1.0", "bundled")));
    }

    public List<PluginRegistryEntry> listBundled() {
        return List.copyOf(bundled);
    }

    public List<PluginRegistryEntry> discoverFrom(Path pluginsDir) {
        discovered.clear();
        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) {
            return List.of();
        }
        try {
            List<PluginManifest> manifests = new PluginManifestLoader().loadFrom(pluginsDir);
            for (PluginManifest manifest : manifests) {
                discovered.add(new PluginRegistryEntry(manifest.name(), manifest.version(), pluginsDir.toString()));
            }
        } catch (Exception e) {
            // Skip invalid plugin directories
        }
        return List.copyOf(discovered);
    }

    public List<PluginRegistryEntry> listDiscovered() {
        return List.copyOf(discovered);
    }

    public List<PluginRegistryEntry> listAll() {
        List<PluginRegistryEntry> all = new ArrayList<>(bundled);
        all.addAll(discovered);
        return List.copyOf(all);
    }
}
