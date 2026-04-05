package ai.binbun.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.model.ToolSpec;

import java.time.Clock;
import java.time.Instant;

public final class ClockTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Clock clock;

    public ClockTool(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        return new ToolSpec("clock", "Return the current UTC timestamp in ISO-8601 format.", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        return Instant.now(clock).toString();
    }
}
