package ai.binbun.agent;

import java.util.List;

public record SessionSnapshot(String sessionId, String parentSessionId, List<StoredMessage> messages, long updatedAtEpochMillis) {
    public SessionSnapshot {
        messages = List.copyOf(messages);
    }

    public SessionSnapshot(String sessionId, List<StoredMessage> messages, long updatedAtEpochMillis) {
        this(sessionId, null, messages, updatedAtEpochMillis);
    }
}
