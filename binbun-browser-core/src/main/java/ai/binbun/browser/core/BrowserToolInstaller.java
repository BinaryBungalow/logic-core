package ai.binbun.browser.core;

import ai.binbun.tools.ToolRegistry;

public final class BrowserToolInstaller {
    private final BrowserToolBridge bridge;

    public BrowserToolInstaller(BrowserToolBridge bridge) {
        this.bridge = bridge;
    }

    public void installInto(ToolRegistry tools) {
        tools.register(new BrowserNavigateTool(bridge))
                .register(new BrowserClickTool(bridge))
                .register(new BrowserTypeTool(bridge))
                .register(new BrowserReadTextTool(bridge));
    }
}
