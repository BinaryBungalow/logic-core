package ai.binbun.gateway;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GatewayServer {
    private final SessionRegistry sessionRegistry;
    private final GatewayEventBus eventBus;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Instant startedAt;

    public GatewayServer(SessionRegistry sessionRegistry, GatewayEventBus eventBus) {
        this.sessionRegistry = sessionRegistry;
        this.eventBus = eventBus;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            startedAt = Instant.now();
            eventBus.publish(new GatewayEvent("gateway.started", startedAt.toString()));
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            eventBus.publish(new GatewayEvent("gateway.stopped", Instant.now().toString()));
        }
    }

    public GatewayStatus status() {
        return new GatewayStatus(running.get(), startedAt == null ? "" : startedAt.toString(), sessionRegistry.size());
    }
}
