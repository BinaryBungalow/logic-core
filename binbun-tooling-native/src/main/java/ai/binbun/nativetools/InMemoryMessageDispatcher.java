package ai.binbun.nativetools;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryMessageDispatcher implements MessageDispatcher {
    private final List<MessageDispatch> sent = new ArrayList<>();

    @Override
    public String dispatch(MessageDispatch dispatch) {
        sent.add(dispatch);
        return "queued:" + dispatch.channel() + ":" + dispatch.destination();
    }

    public List<MessageDispatch> sent() {
        return List.copyOf(sent);
    }
}
