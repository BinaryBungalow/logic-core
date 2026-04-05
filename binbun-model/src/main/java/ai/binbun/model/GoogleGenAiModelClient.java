package ai.binbun.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public final class GoogleGenAiModelClient implements ModelClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;
    private final String apiKey;

    public GoogleGenAiModelClient(URI baseUri, String apiKey) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), baseUri, apiKey);
    }

    public GoogleGenAiModelClient(HttpClient httpClient, ObjectMapper objectMapper, URI baseUri, String apiKey) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUri = baseUri;
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        try {
            var payload = toPayload(request);
            URI endpoint = baseUri.resolve("/v1beta/models/" + request.model() + ":generateContent?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
            var response = httpClient.send(HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofMinutes(2))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());
            return new ChatResponse(extractText(root), extractFinishReason(root), extractToolCalls(root));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Google GenAI", e);
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

    private ObjectNode toPayload(ChatRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        for (ChatRequest.Message message : request.messages()) {
            if ("system".equals(message.role())) {
                continue;
            }
            ObjectNode item = contents.addObject();
            item.put("role", "assistant".equals(message.role()) ? "model" : "user");
            ArrayNode parts = item.putArray("parts");
            if (!message.toolCalls().isEmpty()) {
                for (ToolCall call : message.toolCalls()) {
                    ObjectNode part = parts.addObject();
                    ObjectNode functionCall = part.putObject("functionCall");
                    functionCall.put("name", call.name());
                    functionCall.set("args", parseJson(call.argumentsJson()));
                }
            } else if ("tool".equals(message.role())) {
                ObjectNode part = parts.addObject();
                ObjectNode functionResponse = part.putObject("functionResponse");
                functionResponse.put("name", message.name() == null ? "tool" : message.name());
                functionResponse.set("response", parseJsonObject(message.content()));
            } else {
                parts.addObject().put("text", message.content() == null ? "" : message.content());
            }
        }

        String system = request.messages().stream()
                .filter(m -> "system".equals(m.role()) && m.content() != null)
                .map(ChatRequest.Message::content)
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");
        if (!system.isBlank()) {
            root.putObject("systemInstruction").putArray("parts").addObject().put("text", system);
        }

        if (!request.tools().isEmpty()) {
            ArrayNode tools = root.putArray("tools");
            ObjectNode declarations = tools.addObject();
            ArrayNode functionDeclarations = declarations.putArray("functionDeclarations");
            for (ToolSpec spec : request.tools()) {
                ObjectNode fn = functionDeclarations.addObject();
                fn.put("name", spec.name());
                fn.put("description", spec.description());
                fn.set("parameters", spec.parameters());
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

    private JsonNode parseJsonObject(String text) {
        try {
            String candidate = text == null || text.isBlank() ? "{}" : text;
            JsonNode parsed = objectMapper.readTree(candidate);
            return parsed.isObject() ? parsed : objectMapper.createObjectNode().put("text", text == null ? "" : text);
        } catch (Exception e) {
            return objectMapper.createObjectNode().put("text", text == null ? "" : text);
        }
    }

    private String extractText(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    sb.append(part.path("text").asText(""));
                }
            }
        }
        return sb.toString();
    }

    private String extractFinishReason(JsonNode root) {
        return root.path("candidates").path(0).path("finishReason").asText("unknown");
    }

    private List<ToolCall> extractToolCalls(JsonNode root) {
        List<ToolCall> calls = new ArrayList<>();
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return calls;
        }
        int index = 0;
        for (JsonNode part : parts) {
            if (part.has("functionCall")) {
                JsonNode functionCall = part.path("functionCall");
                try {
                    calls.add(new ToolCall(
                            "google-call-" + index++,
                            functionCall.path("name").asText(""),
                            objectMapper.writeValueAsString(functionCall.path("args"))
                    ));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return calls;
    }
}
