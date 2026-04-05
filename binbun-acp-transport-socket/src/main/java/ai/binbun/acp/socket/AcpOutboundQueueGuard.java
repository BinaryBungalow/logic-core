package ai.binbun.acp.socket;

public final class AcpOutboundQueueGuard {
    private final AcpOutboundQueuePolicy policy;

    public AcpOutboundQueueGuard(AcpOutboundQueuePolicy policy) {
        this.policy = policy;
    }

    public AcpOutboundQueueDecision evaluate(int bufferedEvents) {
        if (bufferedEvents < policy.maxBufferedEvents()) {
            return new AcpOutboundQueueDecision(true, false, false, bufferedEvents);
        }
        if (policy.disconnectSlowConsumers()) {
            return new AcpOutboundQueueDecision(false, false, true, bufferedEvents);
        }
        return new AcpOutboundQueueDecision(false, true, false, bufferedEvents);
    }
}
