package ai.binbun.acp.auth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class SessionTokenManager {
    private final Map<String, SessionToken> tokens = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public SessionTokenManager(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public SessionTokenManager() {
        this(TimeUnit.HOURS.toMillis(24));
    }

    public SessionToken generateToken(String subject, String role, String sessionId) {
        String token = UUID.randomUUID().toString();
        SessionToken st = new SessionToken(token, subject, role, sessionId, System.currentTimeMillis() + ttlMillis);
        tokens.put(token, st);
        return st;
    }

    public boolean validate(String token) {
        SessionToken st = tokens.get(token);
        if (st == null) return false;
        if (System.currentTimeMillis() > st.expiryMillis()) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    public SessionToken getToken(String token) {
        return tokens.get(token);
    }

    public void revoke(String token) {
        tokens.remove(token);
    }

    public void revokeBySession(String sessionId) {
        tokens.entrySet().removeIf(e -> e.getValue().sessionId().equals(sessionId));
    }

    public record SessionToken(String token, String subject, String role, String sessionId, long expiryMillis) {}
}
