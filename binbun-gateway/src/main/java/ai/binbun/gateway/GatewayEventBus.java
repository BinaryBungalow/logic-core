package ai.binbun.gateway;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class GatewayEventBus {
    private final List<Consumer<GatewayEvent>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<GatewayEvent> listener) {
        listeners.add(listener);
    }

    public void publish(GatewayEvent event) {
        for (Consumer<GatewayEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
