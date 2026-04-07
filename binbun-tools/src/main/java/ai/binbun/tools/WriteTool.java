package ai.binbun.tools;

import ai.binbun.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class WriteTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        var properties = schema.putObject("properties");
        properties.putObject("filePath").put("type", "string");
        properties.putObject("content").put("type", "string");
        var required = schema.putArray("required");
        required.add("filePath");
        required.add("content");
        return new ToolSpec("write", "Write content to a file, overwriting existing content", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        try {
            Path path = Paths.get(arguments.get("filePath").asText());
            String content = arguments.get("content").asText();
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            return "Successfully wrote " + content.length() + " bytes to " + path;
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }
}
