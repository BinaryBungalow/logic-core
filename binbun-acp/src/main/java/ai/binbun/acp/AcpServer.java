package ai.binbun.acp;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AcpServer {
    private final Map<String, AcpGateway.AcpSessionHandle> sessions = new ConcurrentHashMap<>();

    public void register(AcpGateway.AcpSessionHandle handle) {
        sessions.put(handle.sessionId(), handle);
    }

    public Optional<AcpGateway.AcpSessionHandle> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public int size() {
        return sessions.size();
    }
}
