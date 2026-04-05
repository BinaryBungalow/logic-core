package ai.binbun.gateway;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class GatewayMethodRegistry {
    private final Map<String, GatewayMethod> methods = new ConcurrentHashMap<>();

    public void register(GatewayMethod method) {
        methods.put(method.name(), method);
    }

    public Optional<GatewayMethod> find(String name) {
        return Optional.ofNullable(methods.get(name));
    }

    public Map<String, GatewayMethod> listAll() {
        return Map.copyOf(methods);
    }
}
