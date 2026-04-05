package ai.binbun.memory;

public interface SessionRepository {
    void save(String sessionId, String json);
    String load(String sessionId);
}
