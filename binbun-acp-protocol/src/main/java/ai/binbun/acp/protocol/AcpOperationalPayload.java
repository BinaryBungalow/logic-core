package ai.binbun.acp.protocol;

import java.util.Map;

public record AcpOperationalPayload(
        String protocolVersion,
        Object operational,
        Object pluginStatus,
        Object deliveryStatus,
        Map<String, Object> metadata
) {
    public AcpOperationalPayload {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
