package ai.binbun.browser.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.binbun.model.ToolSpec;
import ai.binbun.tools.Tool;

public final class BrowserTypeTool implements Tool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final BrowserToolBridge bridge;

    public BrowserTypeTool(BrowserToolBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public ToolSpec spec() {
        ObjectNode properties = OBJECT_MAPPER.createObjectNode();
        properties.set("target", OBJECT_MAPPER.createObjectNode().put("type", "string"));
        properties.set("value", OBJECT_MAPPER.createObjectNode().put("type", "string"));
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", properties);
        schema.set("required", OBJECT_MAPPER.createArrayNode().add("target").add("value"));
        schema.put("additionalProperties", false);
        return new ToolSpec("browser.type", "Type text into a browser target selector", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        return bridge.invoke("browser.type", java.util.Map.of(
                "target", arguments.path("target").asText(""),
                "value", arguments.path("value").asText("")
        )).detail();
    }
}
