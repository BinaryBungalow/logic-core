package ai.binbun.browser.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BrowserToolRegistry {
    private final Map<String, BrowserRegisteredTool> tools = new ConcurrentHashMap<>();

    public BrowserToolRegistry register(BrowserRegisteredTool tool) {
        tools.put(tool.name(), tool);
        return this;
    }

    public Collection<BrowserRegisteredTool> all() {
        return tools.values();
    }
}
