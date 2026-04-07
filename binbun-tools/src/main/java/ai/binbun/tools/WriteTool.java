package ai.binbun.tools;

import ai.binbun.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class WriteTool implements Tool {

    @Override
    public ToolSpec spec() {
        return ToolSpec.builder()
                .name("write")
                .description("Write content to a file, overwriting existing content")
                .parameter("filePath", "string", "Absolute path to the file to write", true)
                .parameter("content", "string", "Content to write to the file", true)
                .build();
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
