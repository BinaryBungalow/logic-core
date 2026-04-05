package ai.binbun.model;

import java.util.List;

public record ChatResponse(String text, String finishReason, List<ToolCall> toolCalls) {
    public ChatResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }
}
