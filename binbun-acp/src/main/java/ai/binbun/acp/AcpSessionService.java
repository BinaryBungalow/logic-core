package ai.binbun.acp;

import ai.binbun.agent.event.AgentEvent;

import java.util.Optional;
import java.util.concurrent.Flow;

public interface AcpSessionService {
    Optional<ManagedSession> find(String sessionId);
    AcpRunResult promptAcp(String sessionId, String input);
    void close(String sessionId);

    record ManagedSession(String id, Flow.Publisher<AgentEvent> events) {}
    record AcpRunResult(String id) {}
}
