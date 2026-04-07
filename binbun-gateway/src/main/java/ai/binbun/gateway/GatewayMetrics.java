package ai.binbun.gateway;

import java.util.concurrent.atomic.AtomicLong;

public final class GatewayMetrics {
    private static final GatewayMetrics INSTANCE = new GatewayMetrics();

    public final AtomicLong requestsTotal = new AtomicLong(0);
    public final AtomicLong requestsFailed = new AtomicLong(0);
    public final AtomicLong requestsRateLimited = new AtomicLong(0);
    public final AtomicLong sessionsActive = new AtomicLong(0);
    public final AtomicLong sessionsTotal = new AtomicLong(0);
    public final AtomicLong messagesInbound = new AtomicLong(0);
    public final AtomicLong messagesOutbound = new AtomicLong(0);
    public final AtomicLong connectionsTotal = new AtomicLong(0);
    public final AtomicLong connectionsActive = new AtomicLong(0);

    private GatewayMetrics() {}

    public static GatewayMetrics getInstance() {
        return INSTANCE;
    }

    public void reset() {
        requestsTotal.set(0);
        requestsFailed.set(0);
        requestsRateLimited.set(0);
        sessionsActive.set(0);
        sessionsTotal.set(0);
        messagesInbound.set(0);
        messagesOutbound.set(0);
        connectionsTotal.set(0);
        connectionsActive.set(0);
    }
}