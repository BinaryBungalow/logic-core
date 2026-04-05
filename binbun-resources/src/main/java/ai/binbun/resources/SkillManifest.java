package ai.binbun.resources;

import java.util.List;

public record SkillManifest(String name, String description, List<String> tags, boolean autoload,
                            String instructions, List<String> requiredTools, String entrypoint) {
    public SkillManifest {
        tags = tags == null ? List.of() : List.copyOf(tags);
        requiredTools = requiredTools == null ? List.of() : List.copyOf(requiredTools);
        description = description == null ? name : description;
        entrypoint = entrypoint == null ? name : entrypoint;
    }
}
