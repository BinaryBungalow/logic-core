package ai.binbun.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.agent.event.MessageCommitted;
import ai.binbun.agent.event.RunFailed;
import ai.binbun.agent.event.TokenDelta;
import ai.binbun.agent.event.ToolFinished;
import ai.binbun.agent.event.ToolRequested;
import ai.binbun.agent.event.ToolStarted;
import ai.binbun.model.ChatRequest;
import ai.binbun.model.ChatResponse;
import ai.binbun.model.ModelClient;
import ai.binbun.model.StreamObserver;
import ai.binbun.model.StreamingChunk;
import ai.binbun.model.ToolCall;
import ai.binbun.model.ToolCallDelta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public final class AgentRuntime {
    private final ModelClient modelClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentRuntime(ModelClient modelClient) {
        this.modelClient = modelClient;
    }

    RunHandle run(DefaultAgentSession session) {
        var run = new RunHandle(UUID.randomUUID().toString());
        Thread.ofVirtual().start(() -> execute(session, run));
        return run;
    }

    private void execute(DefaultAgentSession session, RunHandle run) {
        try {
            executeStreamingLoop(session, run);
        } catch (Exception e) {
            session.emit(new RunFailed(run.id(), e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
        }
    }

    private ChatRequest request(DefaultAgentSession session, boolean stream) {
        return new ChatRequest(session.model(), session.messages().stream()
                .map(m -> new ChatRequest.Message(m.role(), m.content(), m.name(), m.toolCallId(), m.toolCalls()))
                .toList(), stream, session.tools().specs());
    }

    private void executeStreamingLoop(DefaultAgentSession session, RunHandle run) throws InterruptedException {
        for (int turn = 0; turn < 8; turn++) {
            StreamAssembly assembly = streamOnce(session, run);
            if (!assembly.toolCalls().isEmpty()) {
                session.appendAssistantToolCalls(assembly.toolCalls());
                for (ToolCall toolCall : assembly.toolCalls()) {
                    session.emit(new ToolRequested(run.id(), toolCall.name(), toolCall.id(), toolCall.argumentsJson()));
                    session.emit(new ToolStarted(run.id(), toolCall.name(), toolCall.id()));
                    String result = session.tools().execute(toolCall);
                    session.appendToolResult(toolCall.id(), toolCall.name(), result);
                    session.emit(new ToolFinished(run.id(), toolCall.name(), toolCall.id(), result));
                }
                session.saveCheckpoint();
                continue;
            }

            session.appendAssistant(assembly.text());
            session.emit(new MessageCommitted(run.id(), "assistant", assembly.text()));
            session.saveCheckpoint();
            return;
        }
        throw new IllegalStateException("Streaming tool loop exceeded maximum turns");
    }

    private StreamAssembly streamOnce(DefaultAgentSession session, RunHandle run) throws InterruptedException {
        StringBuilder text = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Map<Integer, MutableToolCall> toolCalls = new LinkedHashMap<>();

        modelClient.stream(request(session, true), new StreamObserver() {
            @Override public void onRawLine(String line) {
                session.logRawStreamLine(run.id(), line);
            }
        }).subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            @Override public void onNext(StreamingChunk item) {
                if (!item.textDelta().isEmpty()) {
                    text.append(item.textDelta());
                    session.emit(new TokenDelta(run.id(), item.textDelta()));
                }
                for (ToolCallDelta delta : item.toolCallDeltas()) {
                    MutableToolCall call = toolCalls.computeIfAbsent(delta.index(), ignored -> new MutableToolCall());
                    call.merge(delta);
                }
                if (item.done() || "stop".equals(item.finishReason()) || "tool_calls".equals(item.finishReason())) {
                    done.countDown();
                }
            }
            @Override public void onError(Throwable throwable) {
                error.set(throwable);
                done.countDown();
            }
            @Override public void onComplete() {
                done.countDown();
            }
        });

        done.await();
        if (error.get() != null) {
            return fromCompleteFallback(session);
        }

        List<ToolCall> finalized = toolCalls.values().stream().map(MutableToolCall::toToolCall).toList();
        if (!finalized.isEmpty() && finalized.stream().anyMatch(call -> !isRecoverableToolCall(call))) {
            return fromCompleteFallback(session);
        }
        return new StreamAssembly(text.toString(), finalized);
    }

    private StreamAssembly fromCompleteFallback(DefaultAgentSession session) {
        ChatResponse response = modelClient.complete(request(session, false));
        return new StreamAssembly(response.text(), response.toolCalls());
    }

    private boolean isRecoverableToolCall(ToolCall call) {
        if (call.name() == null || call.name().isBlank()) {
            return false;
        }
        try {
            objectMapper.readTree(call.argumentsJson() == null || call.argumentsJson().isBlank() ? "{}" : call.argumentsJson());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private record StreamAssembly(String text, List<ToolCall> toolCalls) {
    }

    private static final class MutableToolCall {
        private String id = "";
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();

        void merge(ToolCallDelta delta) {
            if (delta.id() != null && !delta.id().isEmpty()) {
                id = delta.id();
            }
            if (delta.nameFragment() != null && !delta.nameFragment().isEmpty()) {
                name.append(delta.nameFragment());
            }
            if (delta.argumentsFragment() != null && !delta.argumentsFragment().isEmpty()) {
                arguments.append(delta.argumentsFragment());
            }
        }

        ToolCall toToolCall() {
            return new ToolCall(id, name.toString(), arguments.length() == 0 ? "{}" : arguments.toString());
        }
    }
}
