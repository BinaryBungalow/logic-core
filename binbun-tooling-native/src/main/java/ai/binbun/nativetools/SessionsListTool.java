package ai.binbun.nativetools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.gateway.GatewayRuntime;
import ai.binbun.model.ToolSpec;
import ai.binbun.tools.Tool;

public final class SessionsListTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final GatewayRuntime gatewayRuntime;

    public SessionsListTool(GatewayRuntime gatewayRuntime) {
        this.gatewayRuntime = gatewayRuntime;
    }

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        return new ToolSpec("sessions.list", "List active registered sessions.", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        try {
            return MAPPER.writeValueAsString(gatewayRuntime.sessions());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize sessions", e);
        }
    }
}
