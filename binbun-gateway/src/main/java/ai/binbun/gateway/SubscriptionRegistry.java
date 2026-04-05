package ai.binbun.gateway;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SubscriptionRegistry {
    private final Map<String, List<Subscription>> subscriptions = new ConcurrentHashMap<>();

    public void subscribe(GatewayConnection connection, String eventType, Consumer<Map<String, Object>> listener) {
        String key = connection.id() + ":" + eventType;
        Subscription sub = new Subscription(connection, eventType, listener);
        subscriptions.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(sub);
    }

    public void unsubscribe(GatewayConnection connection, String eventType) {
        String key = connection.id() + ":" + eventType;
        subscriptions.remove(key);
    }

    public void unsubscribeAll(GatewayConnection connection) {
        subscriptions.keySet().removeIf(k -> k.startsWith(connection.id() + ":"));
    }

    public void publish(String eventType, Map<String, Object> payload) {
        subscriptions.entrySet().stream()
                .filter(e -> e.getKey().endsWith(":" + eventType))
                .flatMap(e -> e.getValue().stream())
                .forEach(sub -> sub.listener().accept(payload));
    }

    public void publish(String eventType, String connectionId, Map<String, Object> payload) {
        String key = connectionId + ":" + eventType;
        List<Subscription> subs = subscriptions.get(key);
        if (subs != null) {
            subs.forEach(sub -> sub.listener().accept(payload));
        }
    }

    public record Subscription(GatewayConnection connection, String eventType, Consumer<Map<String, Object>> listener) {}
}
