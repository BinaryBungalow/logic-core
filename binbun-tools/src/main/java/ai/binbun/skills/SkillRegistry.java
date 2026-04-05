package ai.binbun.skills;

import ai.binbun.resources.PromptManifest;
import ai.binbun.resources.SkillManifest;
import ai.binbun.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

public final class SkillRegistry {
    public SkillActivation activateAll(List<SkillManifest> manifests, List<PromptManifest> prompts,
                                       SkillExecutionContext baseContext, ToolRegistry tools) {
        List<String> systemMessages = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (PromptManifest prompt : prompts) {
            systemMessages.add(prompt.content());
        }

        for (SkillManifest manifest : manifests) {
            RuntimeSkill skill = new ManifestBackedSkill(manifest);
            SkillActivation activation = skill.activate(baseContext, tools);
            systemMessages.addAll(activation.systemMessages());
            warnings.addAll(activation.warnings());
        }
        return new SkillActivation(systemMessages, warnings);
    }
}
