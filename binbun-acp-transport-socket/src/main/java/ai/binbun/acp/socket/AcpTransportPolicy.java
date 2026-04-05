package ai.binbun.acp.socket;

import java.time.Duration;

public record AcpTransportPolicy(
        Duration heartbeatTimeout,
        int outboundQueueCapacity,
        int replayBufferSize,
        Duration ackTimeout,
        int maxConnections
) {
    public static AcpTransportPolicy defaults() {
        return new AcpTransportPolicy(
                Duration.ofSeconds(60),
                128,
                256,
                Duration.ofSeconds(30),
                100
        );
    }

    public static AcpTransportPolicy strict() {
        return new AcpTransportPolicy(
                Duration.ofSeconds(30),
                64,
                128,
                Duration.ofSeconds(15),
                50
        );
    }

    public static AcpTransportPolicy relaxed() {
        return new AcpTransportPolicy(
                Duration.ofSeconds(120),
                256,
                512,
                Duration.ofSeconds(60),
                200
        );
    }
}
