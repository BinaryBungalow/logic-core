package ai.binbun.browser.core;

import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.tools.ToolRegistry;

public final class BrowserPermissionedToolInstaller {
    private final BrowserPermissionService permissionService = new BrowserPermissionService();

    public boolean installFor(ToolRegistry tools, BrowserToolBridge bridge, PluginManifest manifest) {
        if (!permissionService.mayUseBrowser(manifest)) {
            return false;
        }
        new BrowserToolInstaller(bridge).installInto(tools);
        return true;
    }
}
