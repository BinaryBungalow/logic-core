package ai.binbun.acp.socket;

import ai.binbun.acp.protocol.AcpEnvelope;
import ai.binbun.acp.protocol.AcpEnvelopeCodec;
import ai.binbun.acp.protocol.AcpEnvelopes;
import ai.binbun.acp.protocol.AcpOperation;
import ai.binbun.acp.protocol.AcpSequenceTracker;
import ai.binbun.acp.AcpSessionService;
import ai.binbun.agent.event.AgentEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AcpSocketTransportServer implements AutoCloseable {
    private final AcpSessionService sessionService;
    private final AcpSocketProtocolHandler protocolHandler;
    private final AcpEnvelopeCodec codec = new AcpEnvelopeCodec();
    private final ConcurrentHashMap<String, AcpReplayBuffer> replayBuffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AcpSequenceTracker> sessionSequences = new ConcurrentHashMap<>();
    private final int port;
    private final AcpTransportPolicy policy;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public AcpSocketTransportServer(AcpSessionService sessionService, AcpSocketProtocolHandler protocolHandler, int port, AcpTransportPolicy policy) {
        this.sessionService = sessionService;
        this.protocolHandler = protocolHandler;
        this.port = port;
        this.policy = policy == null ? AcpTransportPolicy.defaults() : policy;
    }

    public AcpSocketTransportServer(AcpSessionService sessionService, AcpSocketProtocolHandler protocolHandler, int port) {
        this(sessionService, protocolHandler, port, AcpTransportPolicy.defaults());
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to bind ACP socket transport", e);
        }
        acceptThread = Thread.ofVirtual().start(() -> {
            while (running.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout((int) policy.heartbeatTimeout().plusSeconds(5).toMillis());
                    Thread.ofVirtual().start(() -> handle(socket));
                } catch (IOException e) {
                    if (running.get()) {
                        throw new IllegalStateException("ACP socket accept failed", e);
                    }
                }
            }
        });
    }

    private void handle(Socket socket) {
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            var state = new AcpSocketConnectionState();
            String line;
            while ((line = reader.readLine()) != null) {
                if (state.heartbeatExpired(policy.heartbeatTimeout())) {
                    write(writer, AcpEnvelopes.error(AcpOperation.HEARTBEAT, state.attachedSessionId(), 0L, null, "heartbeat_timeout", "ACP heartbeat timeout"));
                    break;
                }
                AcpEnvelope request;
                try {
                    request = codec.decode(line);
                } catch (Exception decodeFailure) {
                    write(writer, AcpEnvelopes.error(AcpOperation.ERROR, null, 0L, null, "malformed_frame", "Unable to decode ACP frame"));
                    continue;
                }
                var response = protocolHandler.handle(request, state);
                synchronized (writer) {
                    write(writer, response);
                }
                if (response.error() == null && Boolean.TRUE.equals(response.payload().get("attached"))) {
                    if (state.markSubscribed(state.attachedSessionId())) {
                        subscribeToSessionEvents(state.attachedSessionId(), writer, state);
                    }
                    replayBufferedEvents(state.attachedSessionId(), state.lastAcknowledgedSequence(), writer);
                }
                flushQueued(state, writer);
                if (state.disconnectRequested()) {
                    write(writer, AcpEnvelopes.error(AcpOperation.EVENT, state.attachedSessionId(), 0L, null, "backpressure_disconnect", "ACP outbound queue exceeded capacity"));
                    break;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("ACP socket transport handler failed", e);
        }
    }

    private void subscribeToSessionEvents(String sessionId, BufferedWriter writer, AcpSocketConnectionState state) {
        sessionService.find(sessionId).ifPresent(managed -> managed.events().subscribe(new Flow.Subscriber<AgentEvent>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(AgentEvent item) {
                long sequence = sessionSequences.computeIfAbsent(sessionId, ignored -> new AcpSequenceTracker()).next();
                AcpEnvelope event = AcpEnvelopes.event(sessionId, sequence, null, Map.of("eventType", item.getClass().getSimpleName(), "event", item));
                replayBuffers.computeIfAbsent(sessionId, ignored -> new AcpReplayBuffer(policy.replayBufferSize())).add(event);
                if (!state.outboundQueue().offer(event)) {
                    state.requestDisconnect();
                    return;
                }
                synchronized (writer) {
                    try {
                        flushQueued(state, writer);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }

            @Override public void onError(Throwable throwable) { }
            @Override public void onComplete() { }
        }));
    }

    private void replayBufferedEvents(String sessionId, long lastAck, BufferedWriter writer) throws IOException {
        List<AcpEnvelope> replay = replayBuffers.computeIfAbsent(sessionId, ignored -> new AcpReplayBuffer(policy.replayBufferSize())).since(lastAck);
        for (AcpEnvelope envelope : replay) {
            write(writer, envelope);
        }
    }

    private void flushQueued(AcpSocketConnectionState state, BufferedWriter writer) throws IOException {
        for (AcpEnvelope envelope : state.outboundQueue().drain()) {
            write(writer, envelope);
        }
    }

    private void write(BufferedWriter writer, AcpEnvelope payload) throws IOException {
        writer.write(codec.encode(payload));
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() {
        running.set(false);
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
