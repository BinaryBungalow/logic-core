package ai.binbun.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.agent.CheckpointStore;
import ai.binbun.agent.SessionSnapshot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JsonSessionRepository implements CheckpointStore {
    private final Path root;
    private final ObjectMapper objectMapper;

    public JsonSessionRepository(Path root) {
        this(root, new ObjectMapper());
    }

    public JsonSessionRepository(Path root, ObjectMapper objectMapper) {
        this.root = root;
        this.objectMapper = objectMapper;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void save(SessionSnapshot snapshot) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(pathFor(snapshot.sessionId()).toFile(), snapshot);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<SessionSnapshot> load(String sessionId) {
        Path file = pathFor(sessionId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), SessionSnapshot.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<SessionSnapshot> list() {
        try {
            List<SessionSnapshot> snapshots = new ArrayList<>();
            try (var stream = Files.list(root)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                snapshots.add(objectMapper.readValue(path.toFile(), SessionSnapshot.class));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
            return snapshots;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path pathFor(String sessionId) {
        return root.resolve(sessionId + ".json");
    }
}
