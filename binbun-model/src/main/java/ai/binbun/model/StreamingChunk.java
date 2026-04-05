package ai.binbun.model;

import java.util.List;

public record StreamingChunk(String textDelta, boolean done, String finishReason, List<ToolCallDelta> toolCallDeltas) {
    public StreamingChunk {
        finishReason = finishReason == null ? "" : finishReason;
        toolCallDeltas = toolCallDeltas == null ? List.of() : List.copyOf(toolCallDeltas);
    }

    public StreamingChunk(String textDelta, boolean done) {
        this(textDelta, done, "", List.of());
    }
}
