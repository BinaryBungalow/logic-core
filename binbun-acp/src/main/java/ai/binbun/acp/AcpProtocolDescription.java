package ai.binbun.acp;

import java.util.List;

public record AcpProtocolDescription(String transport, List<String> operations) {
    public static AcpProtocolDescription defaults() {
        return new AcpProtocolDescription("websocket", List.of("session.attach", "session.spawn", "session.events", "session.prompt"));
    }
}
