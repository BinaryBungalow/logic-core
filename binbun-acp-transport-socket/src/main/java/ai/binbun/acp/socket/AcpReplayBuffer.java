package ai.binbun.acp.socket;

import ai.binbun.acp.protocol.AcpEnvelope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class AcpReplayBuffer {
    private final int capacity;
    private final Deque<AcpEnvelope> entries = new ArrayDeque<>();

    public AcpReplayBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void add(AcpEnvelope envelope) {
        entries.addLast(envelope);
        while (entries.size() > capacity) {
            entries.removeFirst();
        }
    }

    public synchronized List<AcpEnvelope> since(long lastAcknowledgedSequence) {
        List<AcpEnvelope> result = new ArrayList<>();
        for (AcpEnvelope envelope : entries) {
            if (envelope.sequence() > lastAcknowledgedSequence) {
                result.add(envelope);
            }
        }
        return result;
    }
}
