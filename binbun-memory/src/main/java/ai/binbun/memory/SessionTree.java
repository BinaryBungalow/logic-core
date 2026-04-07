package ai.binbun.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SessionTree {

    private final List<SessionNode> roots = new ArrayList<>();
    private final Map<String, SessionNode> index = new HashMap<>();

    private SessionTree() {}

    public static SessionTree build(List<SessionSnapshot> snapshots) {
        var tree = new SessionTree();

        for (var snapshot : snapshots) {
            var node = new SessionNode(
                    snapshot.sessionId(),
                    snapshot.parentSessionId(),
                    Instant.ofEpochMilli(snapshot.timestamp()),
                    snapshot.messages().size()
            );
            tree.index.put(node.id(), node);
        }

        for (var node : tree.index.values()) {
            if (node.parentId() == null) {
                tree.roots.add(node);
            } else {
                var parent = tree.index.get(node.parentId());
                if (parent != null) {
                    parent.addChild(node);
                } else {
                    tree.roots.add(node);
                }
            }
        }

        return tree;
    }

    public List<SessionNode> roots() {
        return List.copyOf(roots);
    }

    public SessionNode get(String id) {
        return index.get(id);
    }
}
