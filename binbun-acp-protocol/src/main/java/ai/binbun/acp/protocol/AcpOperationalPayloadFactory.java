package ai.binbun.acp.protocol;

import java.util.Map;

public final class AcpOperationalPayloadFactory {
    public AcpOperationalPayload create(Object operational, Object pluginStatus, Object deliveryStatus) {
        return new AcpOperationalPayload(
                AcpProtocolVersion.V1ALPHA1,
                operational,
                pluginStatus,
                deliveryStatus,
                Map.of("kind", "gateway-operational-snapshot")
        );
    }
}
