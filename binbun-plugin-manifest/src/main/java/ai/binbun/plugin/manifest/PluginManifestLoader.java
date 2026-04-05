package ai.binbun.plugin.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PluginManifestLoader {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public List<PluginManifest> loadFrom(Path root) {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var files = Files.list(root)) {
            return files.filter(path -> path.toString().endsWith(".json"))
                    .map(this::readManifest)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private PluginManifest readManifest(Path file) {
        try {
            return objectMapper.readValue(file.toFile(), PluginManifest.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
