package ai.binbun.gateway;

import ai.binbun.acp.auth.AcpPrincipal;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class GatewayConnection {
    private final String id = UUID.randomUUID().toString();
    private final AtomicLong sequenceCounter = new AtomicLong(0);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(true);
    private AcpPrincipal principal;
    private Set<OperatorScope> scopes = Set.of();
    private String attachedSessionId;
    private long lastAckSequence;

    public String id() { return id; }
    public long nextSequence() { return sequenceCounter.incrementAndGet(); }
    public boolean isAuthenticated() { return authenticated.get(); }
    public boolean isConnected() { return connected.get(); }

    public void markAuthenticated(AcpPrincipal principal, Set<OperatorScope> scopes) {
        this.principal = principal;
        this.scopes = scopes;
        this.authenticated.set(true);
    }

    public AcpPrincipal principal() { return principal; }
    public Set<OperatorScope> scopes() { return scopes; }

    public void attachSession(String sessionId) { this.attachedSessionId = sessionId; }
    public String attachedSessionId() { return attachedSessionId; }

    public void acknowledge(long seq) { this.lastAckSequence = seq; }
    public long lastAckSequence() { return lastAckSequence; }

    public void disconnect() { connected.set(false); }
}
