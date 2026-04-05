package ai.binbun.browser.core;

import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.runtime.PluginPermissionGate;

public final class BrowserPermissionService {
    private final PluginPermissionGate permissionGate = new PluginPermissionGate();

    public boolean mayUseBrowser(PluginManifest manifest) {
        return permissionGate.allowsBrowser(manifest);
    }
}
