package ai.binbun.acp;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

public interface AcpGateway {
    String name();
    List<AcpSessionInfo> sessions();
    AcpSessionHandle attachAcpSession(String sessionId);
    void subscribeEvents(Consumer<AcpEvent> consumer);

    record AcpSessionInfo(String id, String model, String status) {}
    record AcpSessionHandle(String sessionId, String endpoint) {}
    record AcpEvent(String type, Object data) {}
}
