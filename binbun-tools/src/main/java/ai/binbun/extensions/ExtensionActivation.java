package ai.binbun.extensions;

import java.util.List;

public record ExtensionActivation(List<String> systemMessages, List<String> warnings, List<String> exportedTools) {
    public ExtensionActivation {
        systemMessages = List.copyOf(systemMessages);
        warnings = List.copyOf(warnings);
        exportedTools = List.copyOf(exportedTools);
    }
}
