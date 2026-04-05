package ai.binbun.gateway;

import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.manifest.PluginManifestLoader;
import ai.binbun.plugin.registry.PluginRegistryService;
import ai.binbun.plugin.runtime.PluginRuntimeManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GatewayPluginBootstrap {
    private final PluginRegistryService registryService;
    private final PluginRuntimeManager runtimeManager;
    private final PluginManifestLoader manifestLoader = new PluginManifestLoader();

    public GatewayPluginBootstrap(PluginRegistryService registryService, PluginRuntimeManager runtimeManager) {
        this.registryService = registryService;
        this.runtimeManager = runtimeManager;
    }

    public List<String> activateFrom(Path manifestRoot) {
        List<PluginManifest> manifests = manifestLoader.loadFrom(manifestRoot);
        if (manifests.isEmpty()) {
            manifests = bundledFallback();
        }
        List<String> activated = new ArrayList<>();
        for (var manifest : manifests) {
            runtimeManager.install(manifest);
            runtimeManager.activate(manifest.name());
            activated.add(manifest.name());
        }
        return activated;
    }

    private List<PluginManifest> bundledFallback() {
        return registryService.listBundled().stream()
                .map(entry -> new PluginManifest(
                        entry.name(),
                        entry.version(),
                        "ai.binbun.coreplugin.CorePluginBootstrap",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        "logic-core-starter"
                ))
                .toList();
    }
}
