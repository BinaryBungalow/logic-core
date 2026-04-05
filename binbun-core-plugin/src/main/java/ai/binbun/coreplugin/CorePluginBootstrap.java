package ai.binbun.coreplugin;

import java.util.List;

public final class CorePluginBootstrap {
    private final CorePluginManifest manifest;

    public CorePluginBootstrap() {
        this(CorePluginManifest.defaults());
    }

    public CorePluginBootstrap(CorePluginManifest manifest) {
        this.manifest = manifest;
    }

    public CorePluginManifest manifest() {
        return manifest;
    }

    public List<String> bootstrapMessages() {
        return manifest.bootstrapMessages();
    }
}
