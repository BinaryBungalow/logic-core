package ai.binbun.nativetools;

public final class ConsoleMessageDispatcher implements MessageDispatcher {
    @Override
    public String dispatch(MessageDispatch dispatch) {
        System.err.println("[message] channel=" + dispatch.channel() + " destination=" + dispatch.destination() + " body=" + dispatch.body());
        return "queued:console:" + dispatch.channel() + ":" + dispatch.destination();
    }
}
