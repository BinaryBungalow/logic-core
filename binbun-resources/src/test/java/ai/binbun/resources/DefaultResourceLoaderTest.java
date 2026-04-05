package ai.binbun.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultResourceLoaderTest {
    @TempDir Path temp;

    @Test
    void loadsSelectedPromptSkillAndExtension() throws Exception {
        Path global = temp.resolve("global");
        Files.createDirectories(global.resolve("prompts"));
        Files.createDirectories(global.resolve("skills"));
        Files.createDirectories(global.resolve("extensions"));
        Files.writeString(global.resolve("prompts/dev.json"), "{\"name\":\"dev\",\"autoload\":true,\"content\":\"prompt\"}");
        Files.writeString(global.resolve("skills/reviewer.json"), "{\"name\":\"reviewer\",\"autoload\":true,\"instructions\":\"skill\"}");
        Files.writeString(global.resolve("extensions/fs.json"), "{\"name\":\"fs\",\"autoload\":true,\"bootstrapMessages\":[\"ext\"]}");

        var loader = new DefaultResourceLoader(global);
        var context = loader.loadRuntimeContext(null, List.of("dev"), List.of("reviewer"), List.of("fs"));
        assertEquals(1, context.prompts().size());
        assertEquals(1, context.skills().size());
        assertEquals(1, context.extensions().size());
    }
}
