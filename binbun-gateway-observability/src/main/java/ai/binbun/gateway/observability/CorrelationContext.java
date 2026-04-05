package ai.binbun.gateway.observability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CorrelationContext {
    private static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public static void set(String key, String value) {
        if (value != null) {
            CONTEXT.get().put(key, value);
        }
    }

    public static String get(String key) {
        return CONTEXT.get().get(key);
    }

    public static String correlationId() {
        return get("correlationId");
    }

    public static String sessionId() {
        return get("sessionId");
    }

    public static String workflowRunId() {
        return get("workflowRunId");
    }

    public static String deliveryJobId() {
        return get("deliveryJobId");
    }

    public static String pluginId() {
        return get("pluginId");
    }

    public static void withCorrelation(String correlationId, String sessionId, Runnable action) {
        String prevCorrelation = get("correlationId");
        String prevSession = get("sessionId");
        try {
            set("correlationId", correlationId);
            set("sessionId", sessionId);
            action.run();
        } finally {
            set("correlationId", prevCorrelation);
            set("sessionId", prevSession);
        }
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
