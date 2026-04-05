package ai.binbun.acp.http;

import ai.binbun.acp.AcpSessionService;
import ai.binbun.acp.AcpSessionService.ManagedSession;
import ai.binbun.acp.protocol.AcpEnvelope;
import ai.binbun.acp.protocol.AcpEnvelopes;
import ai.binbun.acp.protocol.AcpSequenceTracker;
import ai.binbun.acp.socket.AcpReplayBuffer;
import ai.binbun.agent.event.AgentEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;

public final class AcpHttpEventBridge {
    private final AcpSessionService sessionService;
    private final ConcurrentHashMap<String, AcpReplayBuffer> replayBuffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AcpSequenceTracker> sequences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> subscriptions = new ConcurrentHashMap<>();

    public AcpHttpEventBridge(AcpSessionService sessionService) {
        this.sessionService = sessionService;
    }

    public List<AcpEnvelope> replaySince(String sessionId, long lastAckSequence) {
        ensureSubscribed(sessionId);
        return replayBuffers.computeIfAbsent(sessionId, ignored -> new AcpReplayBuffer(256)).since(lastAckSequence);
    }

    private void ensureSubscribed(String sessionId) {
        subscriptions.computeIfAbsent(sessionId, ignored -> {
            ManagedSession managed = sessionService.find(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown session: " + sessionId));
            managed.events().subscribe(new Flow.Subscriber<AgentEvent>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(AgentEvent item) {
                    long sequence = sequences.computeIfAbsent(sessionId, x -> new AcpSequenceTracker()).next();
                    AcpEnvelope event = AcpEnvelopes.event(sessionId, sequence, null, Map.of("eventType", item.getClass().getSimpleName(), "event", item));
                    replayBuffers.computeIfAbsent(sessionId, x -> new AcpReplayBuffer(256)).add(event);
                }

                @Override public void onError(Throwable throwable) { }
                @Override public void onComplete() { }
            });
            return true;
        });
    }
}
