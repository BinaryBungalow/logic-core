package ai.binbun.tools;

import ai.binbun.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class EditTool implements Tool {

    @Override
    public ToolSpec spec() {
        return ToolSpec.builder()
                .name("edit")
                .description("Replace exact string match in file with new content")
                .parameter("filePath", "string", "Absolute path to the file to edit", true)
                .parameter("oldString", "string", "Exact string to replace", true)
                .parameter("newString", "string", "Replacement string", true)
                .build();
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
