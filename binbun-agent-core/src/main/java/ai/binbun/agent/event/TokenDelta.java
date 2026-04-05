package ai.binbun.agent.event;

public record TokenDelta(String runId, String text) implements AgentEvent {}
