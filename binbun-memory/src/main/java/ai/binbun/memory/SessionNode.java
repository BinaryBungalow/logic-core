package ai.binbun.memory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SessionNode {
    private final String id;
    private final String parentId;
    private final Instant createdAt;
    private final int messageCount;
    private final List<SessionNode> children = new ArrayList<>();

    public SessionNode(String id, String parentId, Instant createdAt, int messageCount) {
        this.id = id;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.messageCount = messageCount;
    }

    public String id() {
        return id;
    }

    public String parentId() {
        return parentId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public int messageCount() {
        return messageCount;
    }

    public List<SessionNode> children() {
        return List.copyOf(children);
    }

    public void addChild(SessionNode child) {
        this.children.add(child);
    }
}
