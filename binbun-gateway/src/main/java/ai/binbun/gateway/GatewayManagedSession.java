package ai.binbun.gateway;

import ai.binbun.agent.DefaultAgentSession;

public record GatewayManagedSession(RegisteredSession registered, DefaultAgentSession session) {
}
