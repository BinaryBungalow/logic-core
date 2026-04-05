package ai.binbun.gateway;

import ai.binbun.delivery.core.ConnectorRegistry;
import ai.binbun.delivery.core.InboundMessageEnvelope;

public final class GatewayInboundDeliveryHandler {
    private final ConnectorRegistry connectors;
    private final GatewayPromptRouter router;

    public GatewayInboundDeliveryHandler(ConnectorRegistry connectors, GatewayPromptRouter router) {
        this.connectors = connectors;
        this.router = router;
    }

    public GatewayPromptRun handle(String connectorName, String sessionId, String rawPayload) {
        var connector = connectors.find(connectorName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown connector: " + connectorName));
        InboundMessageEnvelope envelope = connector.normalizeInbound(rawPayload);
        return router.routeToSession(sessionId, envelope);
    }
}
