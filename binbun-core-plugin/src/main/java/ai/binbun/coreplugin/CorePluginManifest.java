package ai.binbun.coreplugin;

import java.util.List;

public record CorePluginManifest(String name, List<String> bootstrapMessages, List<String> nativeTools, List<String> workflowProfiles) {
    public static CorePluginManifest defaults() {
        return new CorePluginManifest(
                "core-personal-assistant",
                List.of(
                        "You are the built-in personal assistant plugin.",
                        "Prefer safe automation, explicit approvals for side effects, and durable memory."
                ),
                List.of("gateway.status", "sessions.list", "cron.schedule", "message.send"),
                List.of("assistant-default")
        );
    }
}
