package ai.binbun.browser.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.binbun.model.ToolSpec;
import ai.binbun.tools.Tool;

public final class BrowserReadTextTool implements Tool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final BrowserToolBridge bridge;

    public BrowserReadTextTool(BrowserToolBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public ToolSpec spec() {
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        ObjectNode properties = OBJECT_MAPPER.createObjectNode();
        properties.set("target", OBJECT_MAPPER.createObjectNode().put("type", "string"));
        schema.put("type", "object");
        schema.set("properties", properties);
        schema.set("required", OBJECT_MAPPER.createArrayNode().add("target"));
        schema.put("additionalProperties", false);
        return new ToolSpec("browser.readText", "Read text from a browser target selector", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        return bridge.invoke("browser.readText", java.util.Map.of("target", arguments.path("target").asText(""))).detail();
    }
}
