package ai.binbun.agent.summary;

import ai.binbun.agent.StoredMessage;
import ai.binbun.agent.memory.MemoryPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryAwareCompactorTest {
    @Test
    void preservesLeadingSystemMessagesAndRecentTail() {
        var compactor = new SummaryAwareCompactor();
        var messages = List.of(
                new StoredMessage("system", "alpha"),
                new StoredMessage("user", "u1"),
                new StoredMessage("assistant", "a1"),
                new StoredMessage("user", "u2"),
                new StoredMessage("assistant", "a2")
        );

        var compacted = compactor.compact(messages, new MemoryPolicy(4, 2, true), older -> "summary");
        assertEquals("system", compacted.get(0).role());
        assertTrue(compacted.get(1).content().contains("summary"));
        assertEquals("user", compacted.get(2).role());
        assertEquals("assistant", compacted.get(3).role());
    }
}
