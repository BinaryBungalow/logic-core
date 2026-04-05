package ai.binbun.integration;

import ai.binbun.browser.core.BrowserRuntimeStatusService;
import ai.binbun.browser.core.BrowserToolBridge;
import ai.binbun.browser.playwright.PlaywrightBrowserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2BrowserRuntimeStatusTest {
    @Test
    void reportsExpandedBrowserRuntimeAvailability() {
        var snapshot = new BrowserRuntimeStatusService().snapshot(new BrowserToolBridge(new PlaywrightBrowserService()));
        assertTrue(snapshot.registeredTools() >= 4);
        assertTrue(snapshot.navigateAvailable());
        assertTrue(snapshot.readTextAvailable());
    }
}
