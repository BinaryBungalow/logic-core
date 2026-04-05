package ai.binbun.acp.socket;

import ai.binbun.acp.auth.AcpPrincipal;

import java.time.Duration;
import java.time.Instant;

public final class AcpSocketConnectionState {
    private final AcpOutboundQueue outboundQueue = new AcpOutboundQueue(128);
    private boolean helloComplete;
    private boolean authenticated;
    private boolean disconnectRequested;
    private AcpPrincipal principal;
    private String attachedSessionId;
    private String subscribedSessionId;
    private long lastAcknowledgedSequence;
    private Instant lastHeartbeatAt = Instant.now();

    public boolean helloComplete() { return helloComplete; }
    public void markHelloComplete() { this.helloComplete = true; touch(); }
    public boolean authenticated() { return authenticated; }
    public void authenticate(AcpPrincipal principal) { this.authenticated = true; this.principal = principal; touch(); }
    public AcpPrincipal principal() { return principal; }
    public String attachedSessionId() { return attachedSessionId; }
    public void attachSession(String sessionId) { this.attachedSessionId = sessionId; touch(); }
    public String subscribedSessionId() { return subscribedSessionId; }
    public boolean markSubscribed(String sessionId) {
        if (sessionId != null && sessionId.equals(subscribedSessionId)) {
            return false;
        }
        this.subscribedSessionId = sessionId;
        return true;
    }
    public long lastAcknowledgedSequence() { return lastAcknowledgedSequence; }
    public void acknowledge(long sequence) { this.lastAcknowledgedSequence = sequence; touch(); }
    public AcpOutboundQueue outboundQueue() { return outboundQueue; }
    public void touch() { this.lastHeartbeatAt = Instant.now(); }
    public boolean heartbeatExpired(Duration timeout) { return Instant.now().isAfter(lastHeartbeatAt.plus(timeout)); }
    public void requestDisconnect() { this.disconnectRequested = true; }
    public boolean disconnectRequested() { return disconnectRequested; }
}
