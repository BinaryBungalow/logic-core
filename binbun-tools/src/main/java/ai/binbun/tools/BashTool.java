package ai.binbun.tools;

import ai.binbun.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class BashTool implements Tool {

    @Override
    public ToolSpec spec() {
        return ToolSpec.builder()
                .name("bash")
                .description("Execute a shell command and return stdout/stderr")
                .parameter("command", "string", "Shell command to execute", true)
                .parameter("timeout", "number", "Timeout in seconds (default 30)", false)
                .build();
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
