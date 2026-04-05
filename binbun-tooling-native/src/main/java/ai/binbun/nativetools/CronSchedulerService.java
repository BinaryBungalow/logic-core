package ai.binbun.nativetools;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CronSchedulerService implements AutoCloseable {
    private final CronExecutor executor;
    private final Duration pollInterval;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public CronSchedulerService(CronExecutor executor, Duration pollInterval) {
        this.executor = executor;
        this.pollInterval = pollInterval;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = Thread.ofVirtual().start(() -> {
            while (running.get()) {
                executor.executeDueSchedules(Instant.now());
                try {
                    Thread.sleep(pollInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    @Override
    public void close() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }
}
