package ai.binbun.nativetools;

import java.util.List;

public final class NativeToolCatalog {
    public List<NativeToolSpec> defaultTools() {
        return List.of(
                new NativeToolSpec("gateway.status", "Inspect gateway runtime status."),
                new NativeToolSpec("sessions.list", "List active registered sessions."),
                new NativeToolSpec("cron.schedule", "Register or inspect scheduled jobs."),
                new NativeToolSpec("message.send", "Dispatch an outbound message via the configured sink.")
        );
    }
}
