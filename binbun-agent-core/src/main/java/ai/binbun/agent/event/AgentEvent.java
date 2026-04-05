package ai.binbun.agent.event;

public sealed interface AgentEvent permits TokenDelta, MessageCommitted, ToolRequested, ToolStarted, ToolFinished, RunFailed {}
