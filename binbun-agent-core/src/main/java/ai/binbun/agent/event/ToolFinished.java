package ai.binbun.agent.event;

public record ToolFinished(String runId, String toolName, String callId, String result) implements AgentEvent {}
