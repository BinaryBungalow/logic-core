package ai.binbun.resources;

import java.util.List;

public record RuntimeResourceContext(List<PromptManifest> prompts, List<SkillManifest> skills,
                                     List<ExtensionManifest> extensions, ResourceCatalog catalog) {
    public RuntimeResourceContext {
        prompts = List.copyOf(prompts);
        skills = List.copyOf(skills);
        extensions = List.copyOf(extensions);
    }
}
