package ai.binbun.extensions;

import java.util.List;

public record ExtensionExecutionContext(String sessionId, String userPrompt, List<String> activeSkills) {
    public ExtensionExecutionContext {
        activeSkills = List.copyOf(activeSkills);
    }
}
