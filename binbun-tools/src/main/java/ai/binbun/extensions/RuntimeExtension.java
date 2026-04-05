package ai.binbun.extensions;

import ai.binbun.tools.ToolRegistry;

public interface RuntimeExtension {
    String name();
    ExtensionActivation activate(ExtensionExecutionContext context, ToolRegistry tools);
}
