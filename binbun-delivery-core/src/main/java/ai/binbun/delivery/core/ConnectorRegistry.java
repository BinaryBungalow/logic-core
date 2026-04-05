package ai.binbun.delivery.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectorRegistry {
    private final Map<String, ChannelConnector> connectors = new ConcurrentHashMap<>();

    public void register(ChannelConnector connector) {
        connectors.put(connector.name(), connector);
    }

    public Optional<ChannelConnector> find(String name) {
        return Optional.ofNullable(connectors.get(name));
    }

    public Collection<ChannelConnector> all() {
        return connectors.values();
    }
}
