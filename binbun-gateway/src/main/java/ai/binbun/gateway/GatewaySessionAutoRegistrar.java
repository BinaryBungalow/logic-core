package ai.binbun.gateway;

import ai.binbun.agent.SessionSnapshot;
import ai.binbun.memory.JsonSessionRepository;

public final class GatewaySessionAutoRegistrar {
    private final JsonSessionRepository sessionRepository;

    public GatewaySessionAutoRegistrar(JsonSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public int registerAll(GatewayRuntime runtime, String owner, String model) {
        int count = 0;
        for (SessionSnapshot snapshot : sessionRepository.list()) {
            runtime.registerSession(new RegisteredSession(snapshot.sessionId(), owner, model, "RECOVERED"));
            count++;
        }
        return count;
    }
}
