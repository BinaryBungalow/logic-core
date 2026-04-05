package ai.binbun.agent;

import java.util.List;

public record SessionSnapshot(String sessionId, List<StoredMessage> messages, long updatedAtEpochMillis) {
    public SessionSnapshot {
        messages = List.copyOf(messages);
    }
}
