package ai.binbun.acp.socket;

public record AcpOutboundQueuePolicy(int maxBufferedEvents, boolean disconnectSlowConsumers) {
    public AcpOutboundQueuePolicy {
        maxBufferedEvents = maxBufferedEvents <= 0 ? 64 : maxBufferedEvents;
    }
}
