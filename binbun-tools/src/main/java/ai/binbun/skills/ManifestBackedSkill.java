package ai.binbun.skills;

import ai.binbun.resources.SkillManifest;
import ai.binbun.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ManifestBackedSkill implements RuntimeSkill {
    private final SkillManifest manifest;

    public ManifestBackedSkill(SkillManifest manifest) {
        this.manifest = manifest;
    }

    @Override
    public String name() {
        return manifest.name();
    }

    @Override
    public SkillActivation activate(SkillExecutionContext context, ToolRegistry tools) {
        List<String> warnings = new ArrayList<>();
        var available = tools.specs().stream().map(spec -> spec.name()).collect(Collectors.toSet());
        for (String required : manifest.requiredTools()) {
            if (!available.contains(required)) {
                warnings.add("Skill '" + manifest.name() + "' expects tool '" + required + "' but it is not registered.");
            }
        }
        String message = "Skill " + manifest.name() + " (entrypoint: " + manifest.entrypoint() + ")\n" + manifest.instructions();
        return new SkillActivation(List.of(message), warnings);
    }
}
