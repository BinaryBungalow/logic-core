package ai.binbun.workflows;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JsonWorkflowRunRepository implements WorkflowRunRepository {
    private final Path root;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonWorkflowRunRepository(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void save(WorkflowRunState state) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(pathFor(state.resumeToken()).toFile(), state);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<WorkflowRunState> find(String resumeToken) {
        Path path = pathFor(resumeToken);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(path.toFile(), WorkflowRunState.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<WorkflowRunState> list() {
        try {
            if (!Files.isDirectory(root)) {
                return List.of();
            }
            List<WorkflowRunState> states = new ArrayList<>();
            try (var stream = Files.list(root)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                states.add(objectMapper.readValue(path.toFile(), WorkflowRunState.class));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
            return states;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path pathFor(String resumeToken) {
        return root.resolve(resumeToken + ".json");
    }
}
