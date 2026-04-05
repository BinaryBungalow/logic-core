package ai.binbun.agent.memory;

public record MemoryPolicy(int maxMessagesBeforeCompaction, int keepLastMessages, boolean preserveLeadingSystemMessages) {
    public static MemoryPolicy defaults() {
        return new MemoryPolicy(40, 12, true);
    }
}
