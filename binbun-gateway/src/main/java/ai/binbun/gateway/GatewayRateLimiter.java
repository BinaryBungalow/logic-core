package ai.binbun.gateway;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class GatewayRateLimiter {
    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(String connectionId) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(connectionId, k -> new Window(now));

        if (now - window.startTime > WINDOW_DURATION.toMillis()) {
            window.reset(now);
        }

        return window.counter.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
    }

    public void cleanupIdleWindows() {
        long cutoff = System.currentTimeMillis() - WINDOW_DURATION.multipliedBy(2).toMillis();
        windows.entrySet().removeIf(entry -> entry.getValue().startTime < cutoff);
    }

    private static final class Window {
        final AtomicInteger counter = new AtomicInteger(0);
        volatile long startTime;

        Window(long startTime) {
            this.startTime = startTime;
        }

        void reset(long time) {
            counter.set(0);
            startTime = time;
        }
    }
}