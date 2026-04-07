package ai.binbun.tools;

import ai.binbun.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class BashTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        var properties = schema.putObject("properties");
        properties.putObject("command").put("type", "string");
        properties.putObject("timeout").put("type", "number");
        var required = schema.putArray("required");
        required.add("command");
        return new ToolSpec("bash", "Execute a shell command and return stdout/stderr", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        try {
            String command = arguments.get("command").asText();
            long timeout = arguments.has("timeout") ? arguments.get("timeout").asLong() : 30;

            Process process = new ProcessBuilder("bash", "-c", command)
                    .redirectErrorStream(true)
                    .start();

            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "Command timed out after " + timeout + " seconds";
            }

            try (InputStream is = process.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }
}
