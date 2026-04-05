package ai.binbun.acp.socket;

import ai.binbun.acp.protocol.AcpEnvelope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class AcpOutboundQueue {
    private final int capacity;
    private final Deque<AcpEnvelope> queue = new ArrayDeque<>();

    public AcpOutboundQueue(int capacity) {
        this.capacity = capacity;
    }

    public synchronized boolean offer(AcpEnvelope envelope) {
        if (queue.size() >= capacity) {
            return false;
        }
        queue.addLast(envelope);
        return true;
    }

    public synchronized List<AcpEnvelope> drain() {
        List<AcpEnvelope> drained = new ArrayList<>(queue);
        queue.clear();
        return drained;
    }
}
