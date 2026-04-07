package ai.binbun.tools;

import ai.binbun.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class EditTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        var properties = schema.putObject("properties");
        properties.putObject("filePath").put("type", "string");
        properties.putObject("oldString").put("type", "string");
        properties.putObject("newString").put("type", "string");
        var required = schema.putArray("required");
        required.add("filePath");
        required.add("oldString");
        required.add("newString");
        return new ToolSpec("edit", "Replace exact string match in file with new content", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        try {
            Path path = Paths.get(arguments.get("filePath").asText());
            String oldString = arguments.get("oldString").asText();
            String newString = arguments.get("newString").asText();

            String content = Files.readString(path);
            if (!content.contains(oldString)) {
                return "Error: oldString not found in file";
            }

            String updated = content.replace(oldString, newString);
            Files.writeString(path, updated);
            return "Successfully edited " + path;
        } catch (Exception e) {
            return "Error editing file: " + e.getMessage();
        }
    }
}
