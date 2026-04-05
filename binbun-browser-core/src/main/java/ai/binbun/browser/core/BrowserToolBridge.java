package ai.binbun.browser.core;

import java.util.Map;

public final class BrowserToolBridge {
    private final BrowserAutomationService browser;
    private final BrowserToolRegistry registry = new BrowserToolRegistry()
            .register(new BrowserRegisteredTool("browser.navigate", "Navigate the browser to a URL"))
            .register(new BrowserRegisteredTool("browser.click", "Click a target selector"))
            .register(new BrowserRegisteredTool("browser.type", "Type text into a target selector"))
            .register(new BrowserRegisteredTool("browser.readText", "Read text from a target selector"));

    public BrowserToolBridge(BrowserAutomationService browser) {
        this.browser = browser;
    }

    public BrowserToolRegistry registry() {
        return registry;
    }

    public BrowserResult invoke(String toolName, Map<String, Object> arguments) {
        String action = toolName.startsWith("browser.") ? toolName.substring("browser.".length()) : toolName;
        String target = String.valueOf(arguments.getOrDefault("target", ""));
        String value = String.valueOf(arguments.getOrDefault("value", ""));
        return browser.execute(new BrowserCommand(action, target, value));
    }
}
