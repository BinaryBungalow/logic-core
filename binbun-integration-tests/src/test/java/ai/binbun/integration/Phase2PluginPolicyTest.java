package ai.binbun.integration;

import ai.binbun.browser.core.BrowserPermissionService;
import ai.binbun.plugin.manifest.PluginCompatibilityValidator;
import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.manifest.PluginPermission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2PluginPolicyTest {
    @Test
    void compatibilityAndBrowserPermissionAreValidated() {
        var compatible = new PluginManifest("plugin-ok", "0.1.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(PluginPermission.BROWSER), List.of(), List.of(), "phase-2");
        var incompatible = new PluginManifest("plugin-bad", "0.1.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "phase-3");
        var compatibility = new PluginCompatibilityValidator();
        var permissions = new BrowserPermissionService();
        assertTrue(compatibility.isCompatible(compatible, "phase-2"));
        assertFalse(compatibility.isCompatible(incompatible, "phase-2"));
        assertTrue(permissions.mayUseBrowser(compatible));
        assertFalse(permissions.mayUseBrowser(incompatible));
    }
}
