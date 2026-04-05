package ai.binbun.acp.socket;

import ai.binbun.acp.auth.AcpAuthService;
import ai.binbun.acp.protocol.AcpEnvelope;
import ai.binbun.acp.protocol.AcpEnvelopes;
import ai.binbun.acp.protocol.AcpOperation;
import ai.binbun.acp.protocol.AcpSequenceTracker;
import ai.binbun.gateway.GatewayAgentSessionService;

import java.util.Map;

public final class AcpSocketProtocolHandler {
    private final AcpAuthService authService;
    private final GatewayAgentSessionService sessionService;
    private final AcpSequenceTracker sequenceTracker = new AcpSequenceTracker();

    public AcpSocketProtocolHandler(AcpAuthService authService, GatewayAgentSessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    public AcpEnvelope handle(AcpEnvelope envelope, AcpSocketConnectionState state) {
        state.touch();
        return switch (envelope.op()) {
            case HELLO -> onHello(envelope, state);
            case AUTH -> onAuth(envelope, state);
            case ATTACH -> onAttach(envelope, state);
            case PROMPT -> onPrompt(envelope, state);
            case ACK -> onAck(envelope, state);
            case HEARTBEAT -> AcpEnvelopes.response(AcpOperation.HEARTBEAT, state.attachedSessionId(), sequenceTracker.next(), envelope.messageId(), Map.of("ok", true));
            case CLOSE -> onClose(envelope, state);
            default -> AcpEnvelopes.error(envelope.op(), envelope.sessionId(), sequenceTracker.next(), envelope.messageId(), "unsupported_op", "Unsupported operation");
        };
    }

    private AcpEnvelope onHello(AcpEnvelope envelope, AcpSocketConnectionState state) {
        state.markHelloComplete();
        return AcpEnvelopes.response(AcpOperation.READY, envelope.sessionId(), sequenceTracker.next(), envelope.messageId(),
                Map.of("protocolVersion", envelope.protocolVersion()));
    }

    private AcpEnvelope onAuth(AcpEnvelope envelope, AcpSocketConnectionState state) {
        if (!state.helloComplete()) {
            return AcpEnvelopes.error(AcpOperation.AUTH, envelope.sessionId(), sequenceTracker.next(), envelope.messageId(), "protocol_error", "HELLO required before AUTH");
        }
        var result = authService.authenticate(String.valueOf(envelope.payload().getOrDefault("token", "")));
        if (!result.authenticated()) {
            return AcpEnvelopes.error(AcpOperation.AUTH, envelope.sessionId(), sequenceTracker.next(), envelope.messageId(), result.errorCode(), result.errorMessage());
        }
        state.authenticate(result.principal());
        return AcpEnvelopes.response(AcpOperation.AUTH, envelope.sessionId(), sequenceTracker.next(), envelope.messageId(),
                Map.of("subject", result.principal().subject(), "role", result.principal().role()));
    }

    private AcpEnvelope onAttach(AcpEnvelope envelope, AcpSocketConnectionState state) {
        if (!state.authenticated()) {
            return AcpEnvelopes.error(AcpOperation.ATTACH, envelope.sessionId(), sequenceTracker.next(), envelope.messageId(), "auth_required", "AUTH required before ATTACH");
        }
        String sessionId = envelope.sessionId();
        sessionService.find(sessionId).orElseThrow(() -> new IllegalArgumentException("Unknown session: " + sessionId));
        state.attachSession(sessionId);
        long lastAck = longValue(envelope.payload().get("lastAckSequence"));
        state.acknowledge(lastAck);
        return AcpEnvelopes.response(AcpOperation.ATTACH, sessionId, sequenceTracker.next(), envelope.messageId(), Map.of("attached", true, "resumeFrom", lastAck));
    }

    private AcpEnvelope onPrompt(AcpEnvelope envelope, AcpSocketConnectionState state) {
        if (!state.authenticated() || state.attachedSessionId() == null) {
            return AcpEnvelopes.error(AcpOperation.PROMPT, envelope.sessionId(), sequenceTracker.next(), envelope.messageId(), "session_required", "ATTACH required before PROMPT");
        }
        String input = String.valueOf(envelope.payload().getOrDefault("input", ""));
        var run = sessionService.prompt(state.attachedSessionId(), input);
        return AcpEnvelopes.response(AcpOperation.PROMPT, state.attachedSessionId(), sequenceTracker.next(), envelope.messageId(), Map.of("runId", run.id()));
    }

    private AcpEnvelope onAck(AcpEnvelope envelope, AcpSocketConnectionState state) {
        long acknowledged = longValue(envelope.payload().getOrDefault("sequence", envelope.sequence()));
        state.acknowledge(acknowledged);
        return AcpEnvelopes.response(AcpOperation.ACK, state.attachedSessionId(), sequenceTracker.next(), envelope.messageId(), Map.of("acknowledged", acknowledged));
    }

    private AcpEnvelope onClose(AcpEnvelope envelope, AcpSocketConnectionState state) {
        if (state.attachedSessionId() != null) {
            sessionService.close(state.attachedSessionId());
        }
        return AcpEnvelopes.response(AcpOperation.CLOSE, state.attachedSessionId(), sequenceTracker.next(), envelope.messageId(), Map.of("closed", true));
    }

    private long longValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
