package ai.binbun.nativetools;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public final class JsonCronExecutionStateRepository implements CronExecutionStateRepository {
    private final Path root;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonCronExecutionStateRepository(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<Instant> lastRunAt(String scheduleId) {
        Path file = root.resolve(scheduleId + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            Map<?, ?> map = objectMapper.readValue(file.toFile(), Map.class);
            return Optional.of(Instant.parse(String.valueOf(map.get("lastRunAt"))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void markExecuted(String scheduleId, Instant ranAt) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve(scheduleId + ".json").toFile(), Map.of("lastRunAt", ranAt.toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
