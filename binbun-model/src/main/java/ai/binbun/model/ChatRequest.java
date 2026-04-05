package ai.binbun.model;

import java.util.List;

public record ChatRequest(String model, List<Message> messages, boolean stream, List<ToolSpec> tools) {
    public ChatRequest {
        messages = List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }

    public record Message(String role, String content, String name, String toolCallId, List<ToolCall> toolCalls) {
        public Message {
            toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        }

        public Message(String role, String content) {
            this(role, content, null, null, List.of());
        }
    }
}
