package ai.binbun.plugin.runtime;

import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.manifest.PluginPermission;

public final class PluginPermissionGate {
    public boolean allowsBrowser(PluginManifest manifest) {
        return manifest.permissions().contains(PluginPermission.BROWSER);
    }
}
