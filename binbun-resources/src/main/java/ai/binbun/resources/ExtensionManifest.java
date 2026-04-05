package ai.binbun.resources;

import java.util.List;

public record ExtensionManifest(String name, String description, boolean autoload,
                                List<String> bootstrapMessages, List<String> requiredTools,
                                List<String> exportedTools, String entrypoint) {
    public ExtensionManifest {
        description = description == null ? name : description;
        bootstrapMessages = bootstrapMessages == null ? List.of() : List.copyOf(bootstrapMessages);
        requiredTools = requiredTools == null ? List.of() : List.copyOf(requiredTools);
        exportedTools = exportedTools == null ? List.of() : List.copyOf(exportedTools);
        entrypoint = entrypoint == null ? name : entrypoint;
    }
}
