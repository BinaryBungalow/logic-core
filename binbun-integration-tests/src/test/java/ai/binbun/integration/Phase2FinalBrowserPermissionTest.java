package ai.binbun.integration;

import ai.binbun.browser.core.BrowserPermissionedToolInstaller;
import ai.binbun.browser.core.BrowserToolBridge;
import ai.binbun.browser.playwright.PlaywrightBrowserService;
import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.manifest.PluginPermission;
import ai.binbun.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2FinalBrowserPermissionTest {
    @Test
    void browserToolsInstallOnlyWhenPermissionAllowsIt() {
        var installer = new BrowserPermissionedToolInstaller();
        var allowed = new PluginManifest("allowed", "1.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(PluginPermission.BROWSER), List.of(), List.of(), "phase-2");
        var denied = new PluginManifest("denied", "1.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "phase-2");
        var allowedTools = new ToolRegistry();
        var deniedTools = new ToolRegistry();
        assertTrue(installer.installFor(allowedTools, new BrowserToolBridge(new PlaywrightBrowserService()), allowed));
        assertFalse(installer.installFor(deniedTools, new BrowserToolBridge(new PlaywrightBrowserService()), denied));
        assertEquals(4, allowedTools.specs().stream().filter(spec -> spec.name().startsWith("browser.")).count());
        assertEquals(0, deniedTools.specs().stream().filter(spec -> spec.name().startsWith("browser.")).count());
    }
}
