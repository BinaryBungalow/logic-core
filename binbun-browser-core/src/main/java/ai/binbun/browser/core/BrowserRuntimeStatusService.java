package ai.binbun.browser.core;

public final class BrowserRuntimeStatusService {
    public BrowserRuntimeStatus snapshot(BrowserToolBridge bridge) {
        var all = bridge.registry().all();
        boolean navigate = all.stream().anyMatch(tool -> tool.name().equals("browser.navigate"));
        boolean readText = all.stream().anyMatch(tool -> tool.name().equals("browser.readText"));
        return new BrowserRuntimeStatus(all.size(), navigate, readText);
    }
}
