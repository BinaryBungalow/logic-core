package ai.binbun.agent;

import ai.binbun.agent.event.AgentEvent;
import ai.binbun.agent.memory.MemoryPolicy;
import ai.binbun.agent.obs.EventSink;
import ai.binbun.agent.obs.NoOpEventSink;
import ai.binbun.agent.summary.SummaryAwareCompactor;
import ai.binbun.agent.summary.SummaryEngine;
import ai.binbun.model.ToolCall;
import ai.binbun.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public final class DefaultAgentSession implements AgentSession {
    private final String id;
    private final String model;
    private final AgentRuntime runtime;
    private final CheckpointStore checkpointStore;
    private final ToolRegistry tools;
    private final SummaryAwareCompactor compactor;
    private final SummaryEngine summaryEngine;
    private final MemoryPolicy memoryPolicy;
    private final EventSink eventSink;
    private final SubmissionPublisher<AgentEvent> publisher = new SubmissionPublisher<>();
    private final List<StoredMessage> messages = new ArrayList<>();

    public DefaultAgentSession(String sessionId, String model, AgentRuntime runtime, CheckpointStore checkpointStore, ToolRegistry tools,
                               SummaryAwareCompactor compactor, SummaryEngine summaryEngine) {
        this(sessionId, model, runtime, checkpointStore, tools, compactor, summaryEngine, MemoryPolicy.defaults(), new NoOpEventSink());
    }

    public DefaultAgentSession(String sessionId, String model, AgentRuntime runtime, CheckpointStore checkpointStore, ToolRegistry tools,
                               SummaryAwareCompactor compactor, SummaryEngine summaryEngine, MemoryPolicy memoryPolicy, EventSink eventSink) {
        this.id = sessionId;
        this.model = model;
        this.runtime = runtime;
        this.checkpointStore = checkpointStore;
        this.tools = tools;
        this.compactor = compactor;
        this.summaryEngine = summaryEngine;
        this.memoryPolicy = memoryPolicy;
        this.eventSink = eventSink == null ? new NoOpEventSink() : eventSink;
        checkpointStore.load(sessionId).ifPresent(snapshot -> messages.addAll(snapshot.messages()));
    }

    @Override
    public String id() {
        return id;
    }

    public String model() {
        return model;
    }

    @Override
    public Flow.Publisher<AgentEvent> events() {
        return publisher;
    }

    @Override
    public RunHandle prompt(String input) {
        messages.add(new StoredMessage("user", input));
        saveCheckpoint();
        return runtime.run(this);
    }

    public void seedSystemMessages(List<String> systemMessages) {
        if (!messages.isEmpty() || systemMessages == null || systemMessages.isEmpty()) {
            return;
        }
        for (String systemMessage : systemMessages) {
            if (systemMessage != null && !systemMessage.isBlank()) {
                messages.add(new StoredMessage("system", systemMessage));
            }
        }
        saveCheckpoint();
    }

    List<StoredMessage> messages() {
        return List.copyOf(messages);
    }

    void appendAssistant(String text) {
        messages.add(new StoredMessage("assistant", text));
    }

    void appendAssistantToolCalls(List<ToolCall> toolCalls) {
        messages.add(new StoredMessage("assistant", null, null, null, toolCalls));
    }

    void appendToolResult(String callId, String toolName, String result) {
        messages.add(new StoredMessage("tool", result, toolName, callId, List.of()));
    }

    void emit(AgentEvent event) {
        eventSink.onNormalizedEvent(id, event);
        publisher.submit(event);
    }

    void logRawStreamLine(String runId, String line) {
        eventSink.onRawStreamLine(id, runId, line);
    }

    void saveCheckpoint() {
        if (messages.size() > memoryPolicy.maxMessagesBeforeCompaction()) {
            List<StoredMessage> compacted = compactor.compact(messages, memoryPolicy, summaryEngine);
            messages.clear();
            messages.addAll(compacted);
        }
        checkpointStore.save(new SessionSnapshot(id, messages, System.currentTimeMillis()));
    }

    @Override
    public ToolRegistry tools() {
        return tools;
    }

    @Override
    public BranchHandle branch(String newSessionId) {
        String targetId = (newSessionId == null || newSessionId.isBlank()) ? UUID.randomUUID().toString() : newSessionId;
        checkpointStore.save(new SessionSnapshot(targetId, messages, System.currentTimeMillis()));
        return new BranchHandle(targetId);
    }

    @Override
    public void compact(int keepLastMessages) {
        List<StoredMessage> compacted = compactor.compact(messages, keepLastMessages, summaryEngine);
        messages.clear();
        messages.addAll(compacted);
        saveCheckpoint();
    }

    @Override
    public void close() {
        publisher.close();
    }
}
