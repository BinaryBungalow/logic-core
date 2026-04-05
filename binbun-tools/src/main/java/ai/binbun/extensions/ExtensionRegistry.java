package ai.binbun.extensions;

import ai.binbun.resources.ExtensionManifest;
import ai.binbun.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

public final class ExtensionRegistry {
    public ExtensionActivation activateAll(List<ExtensionManifest> manifests, ExtensionExecutionContext context, ToolRegistry tools) {
        List<String> systemMessages = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> exportedTools = new ArrayList<>();

        for (ExtensionManifest manifest : manifests) {
            RuntimeExtension extension = new ManifestBackedExtension(manifest);
            ExtensionActivation activation = extension.activate(context, tools);
            systemMessages.addAll(activation.systemMessages());
            warnings.addAll(activation.warnings());
            exportedTools.addAll(activation.exportedTools());
        }
        return new ExtensionActivation(systemMessages, warnings, exportedTools);
    }
}
