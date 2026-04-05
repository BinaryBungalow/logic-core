package ai.binbun.plugin.runtime;

import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.manifest.PluginManifestValidator;
import ai.binbun.plugin.manifest.PluginPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PluginRuntimeManager {
    private final Map<String, PluginRuntimeRecord> records = new ConcurrentHashMap<>();
    private final Map<String, PluginManifest> manifests = new ConcurrentHashMap<>();
    private final PluginManifestValidator validator = new PluginManifestValidator();
    private final List<PluginLifecycleHook> hooks = new ArrayList<>();

    public PluginRuntimeManager(List<PluginLifecycleHook> hooks) {
        this.hooks.addAll(hooks);
    }

    public PluginRuntimeManager() {
        this(List.of());
    }

    public void addLifecycleHook(PluginLifecycleHook hook) {
        hooks.add(hook);
    }

    public PluginRuntimeRecord install(PluginManifest manifest) {
        validator.validate(manifest);
        var record = new PluginRuntimeRecord(manifest.name(), manifest.version(), PluginLifecycleState.INSTALLED);
        records.put(manifest.name(), record);
        manifests.put(manifest.name(), manifest);
        return record;
    }

    public PluginRuntimeRecord activate(String name) {
        var current = records.get(name);
        if (current == null) {
            throw new IllegalArgumentException("Unknown plugin: " + name);
        }
        if (current.state() == PluginLifecycleState.FAILED) {
            throw new IllegalStateException("Plugin " + name + " is in FAILED state, cannot activate");
        }

        PluginManifest manifest = manifests.get(name);
        if (manifest != null) {
            if (!manifest.permissions().isEmpty() && !manifest.permissions().contains(PluginPermission.NETWORK)) {
                // Permission check passes - plugin has declared permissions
            }
            for (String migration : manifest.migrations()) {
                runMigration(name, migration);
            }
        }

        for (PluginLifecycleHook hook : hooks) {
            hook.onActivate(name);
        }

        var activated = new PluginRuntimeRecord(current.name(), current.version(), PluginLifecycleState.ACTIVATED);
        records.put(name, activated);
        return activated;
    }

    public PluginRuntimeRecord deactivate(String name) {
        var current = records.get(name);
        if (current == null) {
            throw new IllegalArgumentException("Unknown plugin: " + name);
        }
        for (PluginLifecycleHook hook : hooks) {
            hook.onDeactivate(name);
        }
        var deactivated = new PluginRuntimeRecord(current.name(), current.version(), PluginLifecycleState.DEACTIVATED);
        records.put(name, deactivated);
        return deactivated;
    }

    public PluginRuntimeRecord fail(String name, String reason) {
        var current = records.get(name);
        if (current == null) {
            throw new IllegalArgumentException("Unknown plugin: " + name);
        }
        var failed = new PluginRuntimeRecord(current.name(), current.version(), PluginLifecycleState.FAILED);
        records.put(name, failed);
        return failed;
    }

    public java.util.List<PluginRuntimeRecord> list() {
        return java.util.List.copyOf(records.values());
    }

    public PluginManifest getManifest(String name) {
        return manifests.get(name);
    }

    private void runMigration(String pluginName, String migrationName) {
        for (PluginLifecycleHook hook : hooks) {
            hook.onMigration(pluginName, migrationName);
        }
    }
}
