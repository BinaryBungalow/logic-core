package ai.binbun.agent;

import java.util.List;
import java.util.Optional;

public interface CheckpointStore {
    void save(SessionSnapshot snapshot);
    Optional<SessionSnapshot> load(String sessionId);
    default List<SessionSnapshot> list() {
        return List.of();
    }
}
