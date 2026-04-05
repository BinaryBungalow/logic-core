package ai.binbun.agent.summary;

import ai.binbun.agent.StoredMessage;
import ai.binbun.model.ToolCall;

import java.util.List;
import java.util.stream.Collectors;

public final class HeuristicSummaryEngine implements SummaryEngine {
    @Override
    public String summarize(List<StoredMessage> messages) {
        if (messages.isEmpty()) {
            return "No earlier conversation to summarize.";
        }
        return messages.stream()
                .map(this::summarizeMessage)
                .filter(s -> !s.isBlank())
                .limit(12)
                .collect(Collectors.joining(" | "));
    }

    private String summarizeMessage(StoredMessage message) {
        if (!message.toolCalls().isEmpty()) {
            String tools = message.toolCalls().stream().map(ToolCall::name).collect(Collectors.joining(", "));
            return message.role() + " requested tools: " + tools;
        }
        String content = message.content() == null ? "" : message.content().replaceAll("\\s+", " ").trim();
        if (content.length() > 120) {
            content = content.substring(0, 117) + "...";
        }
        if (content.isBlank()) {
            return "";
        }
        return message.role() + ": " + content;
    }
}
