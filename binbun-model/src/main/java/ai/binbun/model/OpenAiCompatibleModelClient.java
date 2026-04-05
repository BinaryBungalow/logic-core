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

public final class OpenAiCompatibleModelClient implements ModelClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;
    private final String apiKey;

    public OpenAiCompatibleModelClient(URI baseUri, String apiKey) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), baseUri.resolve("/v1/chat/completions"), apiKey);
    }

    public OpenAiCompatibleModelClient(HttpClient httpClient, ObjectMapper objectMapper, URI endpoint, String apiKey) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        try {
            var payload = toPayload(request, false);
            var httpRequest = requestBuilder(payload).build();
            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            var root = objectMapper.readTree(response.body());
            return new ChatResponse(extractMessageText(root), extractFinishReason(root), extractToolCalls(root));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling model", e);
        }
    }

    @Override
    public Flow.Publisher<StreamingChunk> stream(ChatRequest request) {
        return stream(request, new StreamObserver() {});
    }

    @Override
    public Flow.Publisher<StreamingChunk> stream(ChatRequest request, StreamObserver observer) {
        var publisher = new SubmissionPublisher<StreamingChunk>();
        Thread.ofVirtual().start(() -> {
            try {
                var payload = toPayload(request, true);
                var httpRequest = requestBuilder(payload).build();
                var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
                response.body().forEach(line -> processSseLine(line, publisher, observer));
            } catch (Exception e) {
                publisher.closeExceptionally(e);
                return;
            }
            publisher.close();
        });
        return publisher;
    }

    private HttpRequest.Builder requestBuilder(ObjectNode payload) {
        var builder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8));
        if (!apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder;
    }

    private ObjectNode toPayload(ChatRequest request, boolean stream) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.model());
        root.put("stream", stream);
        ArrayNode messages = root.putArray("messages");
        for (ChatRequest.Message message : request.messages()) {
            ObjectNode item = messages.addObject();
            item.put("role", message.role());
            if (message.content() != null) {
                item.put("content", message.content());
            } else {
                item.putNull("content");
            }
            if (message.name() != null) {
                item.put("name", message.name());
            }
            if (message.toolCallId() != null) {
                item.put("tool_call_id", message.toolCallId());
            }
            if (!message.toolCalls().isEmpty()) {
                ArrayNode toolCalls = item.putArray("tool_calls");
                for (ToolCall call : message.toolCalls()) {
                    ObjectNode toolCall = toolCalls.addObject();
                    toolCall.put("id", call.id());
                    toolCall.put("type", "function");
                    ObjectNode function = toolCall.putObject("function");
                    function.put("name", call.name());
                    function.put("arguments", call.argumentsJson());
                }
            }
        }
        if (!request.tools().isEmpty()) {
            ArrayNode tools = root.putArray("tools");
            for (ToolSpec spec : request.tools()) {
                ObjectNode tool = tools.addObject();
                tool.put("type", "function");
                ObjectNode function = tool.putObject("function");
                function.put("name", spec.name());
                function.put("description", spec.description());
                function.set("parameters", spec.parameters());
            }
        }
        return root;
    }

    private void processSseLine(String rawLine, SubmissionPublisher<StreamingChunk> publisher, StreamObserver observer) {
        if (rawLine == null || rawLine.isBlank()) {
            return;
        }
        observer.onRawLine(rawLine);
        if (!rawLine.startsWith("data:")) {
            return;
        }
        String data = rawLine.substring(5).trim();
        if ("[DONE]".equals(data)) {
            var done = new StreamingChunk("", true, "done", List.of());
            observer.onChunk(done);
            publisher.submit(done);
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choice = root.path("choices").path(0);
            JsonNode delta = choice.path("delta");
            String text = delta.path("content").isMissingNode() || delta.path("content").isNull() ? "" : delta.path("content").asText("");
            String finishReason = choice.path("finish_reason").isNull() ? "" : choice.path("finish_reason").asText("");
            List<ToolCallDelta> toolCallDeltas = extractToolCallDeltas(delta.path("tool_calls"));
            if (!text.isEmpty() || !toolCallDeltas.isEmpty() || !finishReason.isEmpty()) {
                var chunk = new StreamingChunk(text, false, finishReason, toolCallDeltas);
                observer.onChunk(chunk);
                publisher.submit(chunk);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<ToolCallDelta> extractToolCallDeltas(JsonNode toolCallsNode) {
        List<ToolCallDelta> deltas = new ArrayList<>();
        if (!toolCallsNode.isArray()) {
            return deltas;
        }
        for (JsonNode item : toolCallsNode) {
            int index = item.path("index").asInt(0);
            String id = item.path("id").asText("");
            JsonNode function = item.path("function");
            String name = function.path("name").asText("");
            String arguments = function.path("arguments").asText("");
            deltas.add(new ToolCallDelta(index, id, name, arguments));
        }
        return deltas;
    }

    private static String extractMessageText(JsonNode root) {
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        return content.isNull() ? "" : content.asText("");
    }

    private static String extractFinishReason(JsonNode root) {
        return root.path("choices").path(0).path("finish_reason").asText("unknown");
    }

    private static List<ToolCall> extractToolCalls(JsonNode root) {
        List<ToolCall> calls = new ArrayList<>();
        JsonNode toolCalls = root.path("choices").path(0).path("message").path("tool_calls");
        if (!toolCalls.isArray()) {
            return calls;
        }
        for (JsonNode toolCall : toolCalls) {
            calls.add(new ToolCall(
                    toolCall.path("id").asText(""),
                    toolCall.path("function").path("name").asText(""),
                    toolCall.path("function").path("arguments").asText("{}")
            ));
        }
        return calls;
    }
}
