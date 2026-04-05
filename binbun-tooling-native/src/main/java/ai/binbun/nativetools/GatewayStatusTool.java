package ai.binbun.nativetools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.gateway.GatewayRuntime;
import ai.binbun.model.ToolSpec;
import ai.binbun.tools.Tool;

public final class GatewayStatusTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final GatewayRuntime gatewayRuntime;

    public GatewayStatusTool(GatewayRuntime gatewayRuntime) {
        this.gatewayRuntime = gatewayRuntime;
    }

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        return new ToolSpec("gateway.status", "Inspect gateway runtime status.", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        try {
            return MAPPER.writeValueAsString(gatewayRuntime.status());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize gateway status", e);
        }
    }
}
