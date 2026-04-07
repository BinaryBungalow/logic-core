package ai.binbun.tools;

import ai.binbun.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ReadTool implements Tool {

    @Override
    public ToolSpec spec() {
        return ToolSpec.builder()
                .name("read")
                .description("Read the contents of a file")
                .parameter("filePath", "string", "Absolute path to the file to read", true)
                .build();
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
