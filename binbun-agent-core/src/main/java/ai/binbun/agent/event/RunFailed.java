package ai.binbun.agent.event;

public record RunFailed(String runId, String reason) implements AgentEvent {}
