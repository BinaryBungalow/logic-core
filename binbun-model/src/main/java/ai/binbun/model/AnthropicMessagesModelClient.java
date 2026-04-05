package ai.binbun.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public final class AnthropicMessagesModelClient implements ModelClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String apiKey;

    public AnthropicMessagesModelClient(URI baseUri, String apiKey) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), baseUri.resolve("/v1/messages"), apiKey);
    }

    public AnthropicMessagesModelClient(HttpClient httpClient, ObjectMapper objectMapper, URI endpoint, String apiKey) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        try {
            var payload = toPayload(request);
            var response = httpClient.send(requestBuilder(payload).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            return new ChatResponse(extractText(root), extractStopReason(root), extractToolCalls(root));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Anthropic", e);
        }
    }

    @Override
    public Flow.Publisher<StreamingChunk> stream(ChatRequest request) {
        var publisher = new SubmissionPublisher<StreamingChunk>();
        Thread.ofVirtual().start(() -> {
            ChatResponse response = complete(request);
            if (!response.text().isEmpty()) {
                publisher.submit(new StreamingChunk(response.text(), false, response.finishReason(), List.of()));
            }
            if (!response.toolCalls().isEmpty()) {
                List<ToolCallDelta> deltas = new ArrayList<>();
                for (int i = 0; i < response.toolCalls().size(); i++) {
                    ToolCall call = response.toolCalls().get(i);
                    deltas.add(new ToolCallDelta(i, call.id(), call.name(), call.argumentsJson()));
                }
                publisher.submit(new StreamingChunk("", false, response.finishReason(), deltas));
            }
            publisher.submit(new StreamingChunk("", true, response.finishReason(), List.of()));
            publisher.close();
        });
        return publisher;
    }

    private HttpRequest.Builder requestBuilder(ObjectNode payload) {
        var builder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMinutes(2))
                .header("content-type", "application/json")
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8));
        if (!apiKey.isBlank()) {
            builder.header("x-api-key", apiKey);
        }
        return builder;
    }

    private ObjectNode toPayload(ChatRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.model());
        root.put("max_tokens", 4096);

        String system = request.messages().stream()
                .filter(m -> "system".equals(m.role()) && m.content() != null)
                .map(ChatRequest.Message::content)
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");
        if (!system.isBlank()) {
            root.put("system", system);
        }

        ArrayNode messages = root.putArray("messages");
        for (ChatRequest.Message message : request.messages()) {
            if ("system".equals(message.role())) {
                continue;
            }
            ObjectNode item = messages.addObject();
            item.put("role", mapRole(message.role()));
            ArrayNode content = item.putArray("content");
            if (!message.toolCalls().isEmpty()) {
                for (ToolCall call : message.toolCalls()) {
                    ObjectNode toolUse = content.addObject();
                    toolUse.put("type", "tool_use");
                    toolUse.put("id", call.id());
                    toolUse.put("name", call.name());
                    toolUse.set("input", parseJson(call.argumentsJson()));
                }
            } else if ("tool".equals(message.role())) {
                ObjectNode toolResult = content.addObject();
                toolResult.put("type", "tool_result");
                toolResult.put("tool_use_id", message.toolCallId());
                toolResult.put("content", message.content() == null ? "" : message.content());
            } else {
                ObjectNode text = content.addObject();
                text.put("type", "text");
                text.put("text", message.content() == null ? "" : message.content());
            }
        }

        if (!request.tools().isEmpty()) {
            ArrayNode tools = root.putArray("tools");
            for (ToolSpec spec : request.tools()) {
                ObjectNode tool = tools.addObject();
                tool.put("name", spec.name());
                tool.put("description", spec.description());
                tool.set("input_schema", spec.parameters());
            }
        }
        return root;
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String mapRole(String role) {
        return switch (role) {
            case "assistant" -> "assistant";
            default -> "user";
        };
    }

    private String extractText(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        JsonNode content = root.path("content");
        if (content.isArray()) {
            for (JsonNode item : content) {
                if ("text".equals(item.path("type").asText())) {
                    sb.append(item.path("text").asText(""));
                }
            }
        }
        return sb.toString();
    }

    private String extractStopReason(JsonNode root) {
        return root.path("stop_reason").asText("unknown");
    }

    private List<ToolCall> extractToolCalls(JsonNode root) {
        List<ToolCall> calls = new ArrayList<>();
        JsonNode content = root.path("content");
        if (!content.isArray()) {
            return calls;
        }
        for (JsonNode item : content) {
            if ("tool_use".equals(item.path("type").asText())) {
                try {
                    calls.add(new ToolCall(
                            item.path("id").asText(""),
                            item.path("name").asText(""),
                            objectMapper.writeValueAsString(item.path("input"))
                    ));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return calls;
    }
}
