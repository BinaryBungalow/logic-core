package ai.binbun.nativetools;

import java.nio.file.Path;

public final class MessageDispatcherFactory {
    public MessageDispatcher create(Path home) {
        String mode = System.getenv().getOrDefault("PI_MESSAGE_DISPATCH", "file");
        return switch (mode) {
            case "console" -> new ConsoleMessageDispatcher();
            case "webhook" -> new WebhookMessageDispatcher(System.getenv().getOrDefault("PI_MESSAGE_WEBHOOK_URL", "http://127.0.0.1:9999/messages"));
            default -> new FileMessageDispatcher(home.resolve("outbox").resolve("messages.jsonl"));
        };
    }
}
