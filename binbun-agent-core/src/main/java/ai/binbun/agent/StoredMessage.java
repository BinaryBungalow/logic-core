package ai.binbun.agent;

import ai.binbun.model.ToolCall;

import java.util.List;

public record StoredMessage(String role, String content, String name, String toolCallId, List<ToolCall> toolCalls) {
    public StoredMessage {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public StoredMessage(String role, String content) {
        this(role, content, null, null, List.of());
    }
}
