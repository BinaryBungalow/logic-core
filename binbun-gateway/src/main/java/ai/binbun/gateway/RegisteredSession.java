package ai.binbun.gateway;

public record RegisteredSession(String sessionId, String owner, String model, String state) {
}
