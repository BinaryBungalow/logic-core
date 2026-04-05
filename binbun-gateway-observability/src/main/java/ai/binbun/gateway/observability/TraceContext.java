package ai.binbun.gateway.observability;

public record TraceContext(String correlationId, String sessionId, String workflowRunId, String deliveryJobId, String pluginId) {
}
