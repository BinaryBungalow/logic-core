package ai.binbun.integration;

import ai.binbun.acp.protocol.AcpOperationalPayloadFactory;
import ai.binbun.delivery.core.DeliveryRuntimeStatus;
import ai.binbun.gateway.health.GatewayHealthService;
import ai.binbun.gateway.health.GatewayOperationalSnapshotService;
import ai.binbun.gateway.recovery.GatewayRecoveryCoordinator;
import ai.binbun.plugin.manifest.PluginRuntimeStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Phase2BroadAcpPayloadTest {
    @Test
    void buildsOperationalPayloadAcrossWorkstreams() {
        var operational = new GatewayOperationalSnapshotService(new GatewayHealthService(), new GatewayRecoveryCoordinator())
                .snapshot(true, true, false);
        var plugin = new PluginRuntimeStatus("plugin-d", false, 2);
        var delivery = new DeliveryRuntimeStatus(3, 1, 1, 1, 1, true);
        var payload = new AcpOperationalPayloadFactory().create(operational, plugin, delivery);
        assertEquals("1.alpha1", payload.protocolVersion());
        assertEquals("gateway-operational-snapshot", payload.metadata().get("kind"));
    }
}
