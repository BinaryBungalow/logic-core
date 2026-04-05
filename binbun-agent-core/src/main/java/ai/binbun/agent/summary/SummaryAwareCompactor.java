package ai.binbun.agent.summary;

import ai.binbun.agent.StoredMessage;
import ai.binbun.agent.memory.MemoryPolicy;

import java.util.ArrayList;
import java.util.List;

public final class SummaryAwareCompactor {
    public List<StoredMessage> compact(List<StoredMessage> original, int keepLastMessages, SummaryEngine summaryEngine) {
        return compact(original, new MemoryPolicy(Integer.MAX_VALUE, keepLastMessages, true), summaryEngine);
    }

    public List<StoredMessage> compact(List<StoredMessage> original, MemoryPolicy policy, SummaryEngine summaryEngine) {
        if (original.size() <= policy.keepLastMessages()) {
            return List.copyOf(original);
        }

        int systemPrefix = 0;
        if (policy.preserveLeadingSystemMessages()) {
            while (systemPrefix < original.size() && "system".equals(original.get(systemPrefix).role())) {
                systemPrefix++;
            }
        }

        int tailStart = Math.max(systemPrefix, original.size() - policy.keepLastMessages());
        List<StoredMessage> prefix = List.copyOf(original.subList(0, systemPrefix));
        List<StoredMessage> older = List.copyOf(original.subList(systemPrefix, tailStart));
        List<StoredMessage> recent = List.copyOf(original.subList(tailStart, original.size()));

        if (older.isEmpty()) {
            return List.copyOf(original);
        }

        String summary = summaryEngine.summarize(older);
        List<StoredMessage> compacted = new ArrayList<>();
        compacted.addAll(prefix);
        compacted.add(new StoredMessage("system", "Conversation summary: " + summary));
        compacted.addAll(recent);
        return compacted;
    }
}
