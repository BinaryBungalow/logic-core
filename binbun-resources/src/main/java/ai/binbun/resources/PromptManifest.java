package ai.binbun.resources;

import java.util.List;

public record PromptManifest(String name, String title, List<String> tags, boolean autoload, String content) {
    public PromptManifest {
        tags = tags == null ? List.of() : List.copyOf(tags);
        title = title == null ? name : title;
    }
}
