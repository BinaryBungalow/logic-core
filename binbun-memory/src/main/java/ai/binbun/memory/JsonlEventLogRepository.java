package ai.binbun.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.agent.event.AgentEvent;
import ai.binbun.agent.obs.EventSink;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonlEventLogRepository implements EventSink {
    private final Path root;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonlEventLogRepository(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void onRawStreamLine(String sessionId, String runId, String line) {
        write(sessionId, mapOf(
                "timestamp", Instant.now().toString(),
                "kind", "raw_stream_line",
                "sessionId", sessionId,
                "runId", runId,
                "line", line
        ));
    }

    @Override
    public void onNormalizedEvent(String sessionId, AgentEvent event) {
        write(sessionId, mapOf(
                "timestamp", Instant.now().toString(),
                "kind", "normalized_event",
                "sessionId", sessionId,
                "eventType", event.getClass().getSimpleName(),
                "event", event
        ));
    }

    private void write(String sessionId, Map<String, Object> value) {
        try {
            String json = objectMapper.writeValueAsString(value) + "\n";
            Files.writeString(pathFor(sessionId), json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path pathFor(String sessionId) {
        return root.resolve(sessionId + ".events.jsonl");
    }

    private static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
