package ai.binbun.gateway;

import ai.binbun.delivery.core.InboundMessageEnvelope;

public final class GatewayDeliveryRouter implements GatewayPromptRouter {
    private final GatewayAgentSessionService sessions;

    public GatewayDeliveryRouter(GatewayAgentSessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public GatewayPromptRun routeToSession(String sessionId, InboundMessageEnvelope envelope) {
        return sessions.prompt(sessionId, envelope.text());
    }
}
