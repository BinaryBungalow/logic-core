package ai.binbun.gateway;

import ai.binbun.delivery.core.InboundMessageEnvelope;

@FunctionalInterface
public interface GatewayPromptRouter {
    GatewayPromptRun routeToSession(String sessionId, InboundMessageEnvelope envelope);
}
