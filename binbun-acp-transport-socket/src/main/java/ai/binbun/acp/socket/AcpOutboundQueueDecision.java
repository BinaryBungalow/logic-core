package ai.binbun.acp.socket;

public record AcpOutboundQueueDecision(boolean accepted, boolean throttled, boolean disconnect, int bufferedEvents) {
}
