package ai.binbun.model;

public record ToolCallDelta(int index, String id, String nameFragment, String argumentsFragment) {}
