package ai.binbun.agent.obs;

import ai.binbun.agent.event.AgentEvent;

public interface EventSink {
    default void onRawStreamLine(String sessionId, String runId, String line) {
    }

    default void onNormalizedEvent(String sessionId, AgentEvent event) {
    }
}
