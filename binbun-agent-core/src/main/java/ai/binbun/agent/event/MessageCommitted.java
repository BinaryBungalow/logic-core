package ai.binbun.agent.event;

public record MessageCommitted(String runId, String role, String text) implements AgentEvent {
    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "";
    }
}
