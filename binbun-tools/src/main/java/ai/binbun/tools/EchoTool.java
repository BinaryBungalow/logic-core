package ai.binbun.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.model.ToolSpec;

public final class EchoTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        var properties = schema.putObject("properties");
        properties.putObject("text").put("type", "string");
        var required = schema.putArray("required");
        required.add("text");
        return new ToolSpec("echo", "Echo back the provided text.", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        return arguments.path("text").asText("");
    }
}
