package ai.binbun.extensions;

import ai.binbun.resources.ExtensionManifest;
import ai.binbun.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ManifestBackedExtension implements RuntimeExtension {
    private final ExtensionManifest manifest;

    public ManifestBackedExtension(ExtensionManifest manifest) {
        this.manifest = manifest;
    }

    @Override
    public String name() {
        return manifest.name();
    }

    @Override
    public ExtensionActivation activate(ExtensionExecutionContext context, ToolRegistry tools) {
        List<String> warnings = new ArrayList<>();
        var available = tools.specs().stream().map(spec -> spec.name()).collect(Collectors.toSet());
        for (String required : manifest.requiredTools()) {
            if (!available.contains(required)) {
                warnings.add("Extension '" + manifest.name() + "' requires tool '" + required + "' but it is not registered.");
            }
        }
        List<String> messages = new ArrayList<>();
        messages.add("Extension " + manifest.name() + " (entrypoint: " + manifest.entrypoint() + ") activated.");
        messages.addAll(manifest.bootstrapMessages());
        return new ExtensionActivation(messages, warnings, manifest.exportedTools());
    }
}
