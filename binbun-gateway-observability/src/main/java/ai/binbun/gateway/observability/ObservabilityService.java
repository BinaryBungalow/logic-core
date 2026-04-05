package ai.binbun.gateway.observability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ObservabilityService {
    private final MetricsCollector metrics;
    private final StructuredLogger logger;
    private final Map<String, Long> counters = new ConcurrentHashMap<>();

    public ObservabilityService(MetricsCollector metrics, StructuredLogger logger) {
        this.metrics = metrics;
        this.logger = logger;
    }

    public ObservabilityService() {
        this(new NoOpMetricsCollector(), new StructuredLogger("gateway"));
    }

    public TraceContext newTrace(String sessionId) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationContext.set("correlationId", correlationId);
        CorrelationContext.set("sessionId", sessionId);
        logger.info("trace started", Map.of("correlationId", correlationId, "sessionId", sessionId));
        metrics.incrementCounter("trace.started", "sessionId", sessionId);
        return new TraceContext(correlationId, sessionId, null, null, null);
    }

    public TraceContext withWorkflowRun(TraceContext ctx, String workflowRunId) {
        CorrelationContext.set("workflowRunId", workflowRunId);
        return new TraceContext(ctx.correlationId(), ctx.sessionId(), workflowRunId, ctx.deliveryJobId(), ctx.pluginId());
    }

    public TraceContext withDeliveryJob(TraceContext ctx, String deliveryJobId) {
        CorrelationContext.set("deliveryJobId", deliveryJobId);
        return new TraceContext(ctx.correlationId(), ctx.sessionId(), ctx.workflowRunId(), deliveryJobId, ctx.pluginId());
    }

    public TraceContext withPlugin(TraceContext ctx, String pluginId) {
        CorrelationContext.set("pluginId", pluginId);
        return new TraceContext(ctx.correlationId(), ctx.sessionId(), ctx.workflowRunId(), ctx.deliveryJobId(), pluginId);
    }

    public void recordMetric(String name, double value, String... tags) {
        metrics.recordHistogram(name, value, tags);
    }

    public void incrementMetric(String name, String... tags) {
        metrics.incrementCounter(name, tags);
    }

    private static final class NoOpMetricsCollector implements MetricsCollector {
        @Override public void incrementCounter(String name, String... tags) {}
        @Override public void recordHistogram(String name, double value, String... tags) {}
        @Override public void setGauge(String name, double value, String... tags) {}
    }
}
