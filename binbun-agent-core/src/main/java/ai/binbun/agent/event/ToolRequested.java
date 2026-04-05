package ai.binbun.agent.event;

public record ToolRequested(String runId, String toolName, String callId, String argumentsJson) implements AgentEvent {}
