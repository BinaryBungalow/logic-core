package ai.binbun.extensions;

import ai.binbun.resources.ExtensionManifest;
import ai.binbun.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExtensionRegistryTest {
    @Test
    void activatesManifestBackedExtension() {
        var registry = new ExtensionRegistry();
        var manifest = new ExtensionManifest("fs", "filesystem", true, List.of("boot"), List.of("missing"), List.of("ls"), "fs.main");
        var activation = registry.activateAll(List.of(manifest), new ExtensionExecutionContext("s", "prompt", List.of()), new ToolRegistry());
        assertEquals(1, activation.exportedTools().size());
        assertFalse(activation.warnings().isEmpty());
    }
}
