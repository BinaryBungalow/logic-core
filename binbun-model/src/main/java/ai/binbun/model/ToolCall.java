package ai.binbun.model;

public record ToolCall(String id, String name, String argumentsJson) {}
