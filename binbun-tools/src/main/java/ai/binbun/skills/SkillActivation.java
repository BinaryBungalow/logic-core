package ai.binbun.skills;

import java.util.List;

public record SkillActivation(List<String> systemMessages, List<String> warnings) {
    public SkillActivation {
        systemMessages = List.copyOf(systemMessages);
        warnings = List.copyOf(warnings);
    }
}
