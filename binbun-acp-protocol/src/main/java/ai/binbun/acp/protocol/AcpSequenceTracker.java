package ai.binbun.acp.protocol;

import java.util.concurrent.atomic.AtomicLong;

public final class AcpSequenceTracker {
    private final AtomicLong sequence = new AtomicLong(0);

    public long next() {
        return sequence.incrementAndGet();
    }
}
