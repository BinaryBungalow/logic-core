package ai.binbun.agent.summary;

import ai.binbun.agent.StoredMessage;

import java.util.List;

public interface SummaryEngine {
    String summarize(List<StoredMessage> messages);
}
