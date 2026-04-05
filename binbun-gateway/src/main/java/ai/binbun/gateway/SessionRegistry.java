package ai.binbun.gateway;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionRegistry {
    private final Map<String, RegisteredSession> sessions = new ConcurrentHashMap<>();

    public void register(RegisteredSession session) {
        sessions.put(session.sessionId(), session);
    }

    public Optional<RegisteredSession> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Collection<RegisteredSession> list() {
        return sessions.values();
    }

    public int size() {
        return sessions.size();
    }
}
