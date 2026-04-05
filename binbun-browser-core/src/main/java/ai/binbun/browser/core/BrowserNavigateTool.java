package ai.binbun.browser.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.model.ToolSpec;
import ai.binbun.tools.Tool;

public final class BrowserNavigateTool implements Tool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final BrowserToolBridge bridge;

    public BrowserNavigateTool(BrowserToolBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public ToolSpec spec() {
        JsonNode schema = OBJECT_MAPPER.createObjectNode()
                .put("type", "object")
                .set("properties", OBJECT_MAPPER.createObjectNode()
                        .set("target", OBJECT_MAPPER.createObjectNode().put("type", "string")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) schema).set("required", OBJECT_MAPPER.createArrayNode().add("target"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) schema).put("additionalProperties", false);
        return new ToolSpec("browser.navigate", "Navigate the browser to a URL", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        return bridge.invoke("browser.navigate", java.util.Map.of("target", arguments.path("target").asText(""))).detail();
    }
}
