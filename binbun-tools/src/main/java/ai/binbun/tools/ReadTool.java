package ai.binbun.tools;

import ai.binbun.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ReadTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        var properties = schema.putObject("properties");
        properties.putObject("filePath").put("type", "string");
        var required = schema.putArray("required");
        required.add("filePath");
        return new ToolSpec("read", "Read the contents of a file", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        try {
            Path path = Paths.get(arguments.get("filePath").asText());
            return Files.readString(path);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }
}
