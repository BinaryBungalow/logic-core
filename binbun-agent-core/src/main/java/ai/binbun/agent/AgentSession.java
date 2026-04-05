package ai.binbun.agent;

import ai.binbun.agent.event.AgentEvent;
import ai.binbun.tools.ToolRegistry;

import java.util.concurrent.Flow;

public interface AgentSession extends AutoCloseable {
    String id();
    Flow.Publisher<AgentEvent> events();
    RunHandle prompt(String input);
    ToolRegistry tools();
    BranchHandle branch(String newSessionId);
    void compact(int keepLastMessages);
    @Override void close();
}
