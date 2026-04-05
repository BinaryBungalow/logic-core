package ai.binbun.skills;

import java.util.List;

public record SkillExecutionContext(String sessionId, String userPrompt, List<String> activePrompts) {
    public SkillExecutionContext {
        activePrompts = List.copyOf(activePrompts);
    }
}
