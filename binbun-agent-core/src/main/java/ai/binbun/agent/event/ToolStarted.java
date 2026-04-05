package ai.binbun.agent.event;

public record ToolStarted(String runId, String toolName, String callId) implements AgentEvent {}
