package ai.binbun.agent.summary;

import ai.binbun.agent.StoredMessage;
import ai.binbun.model.ChatRequest;
import ai.binbun.model.ModelClient;

import java.util.ArrayList;
import java.util.List;

public final class ModelBackedSummaryEngine implements SummaryEngine {
    private final ModelClient modelClient;
    private final String model;

    public ModelBackedSummaryEngine(ModelClient modelClient, String model) {
        this.modelClient = modelClient;
        this.model = model;
    }

    @Override
    public String summarize(List<StoredMessage> messages) {
        if (messages.isEmpty()) {
            return "No earlier conversation to summarize.";
        }

        StringBuilder transcript = new StringBuilder();
        for (StoredMessage message : messages) {
            transcript.append(message.role()).append(": ");
            if (!message.toolCalls().isEmpty()) {
                transcript.append("tool calls=").append(message.toolCalls());
            } else {
                transcript.append(message.content() == null ? "" : message.content());
            }
            transcript.append("\n");
        }

        List<ChatRequest.Message> prompt = new ArrayList<>();
        prompt.add(new ChatRequest.Message("system", "Summarize the earlier conversation into a compact, implementation-useful memory. Preserve goals, decisions, constraints, tool results, and unresolved items in under 180 words."));
        prompt.add(new ChatRequest.Message("user", transcript.toString()));
        return modelClient.complete(new ChatRequest(model, prompt, false, List.of())).text();
    }
}
