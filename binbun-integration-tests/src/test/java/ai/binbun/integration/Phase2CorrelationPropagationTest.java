package ai.binbun.integration;

import ai.binbun.gateway.observability.CorrelationContext;
import ai.binbun.gateway.observability.MetricsCollector;
import ai.binbun.gateway.observability.ObservabilityService;
import ai.binbun.gateway.observability.StructuredLogger;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Phase2CorrelationPropagationTest {
    @Test
    void correlationContextPropagatesThroughThreadLocal() {
        CorrelationContext.set("correlationId", "corr-123");
        CorrelationContext.set("sessionId", "sess-456");

        assertEquals("corr-123", CorrelationContext.correlationId());
        assertEquals("sess-456", CorrelationContext.sessionId());

        CorrelationContext.clear();
        assertNull(CorrelationContext.correlationId());
    }

    @Test
    void observabilityServiceCreatesTraceWithContext() {
        var service = new ObservabilityService();
        var trace = service.newTrace("sess-1");

        assertNotNull(trace.correlationId());
        assertEquals("sess-1", trace.sessionId());
        assertEquals("sess-1", CorrelationContext.sessionId());
        assertEquals(trace.correlationId(), CorrelationContext.correlationId());

        CorrelationContext.clear();
    }

    @Test
    void traceContextEnrichmentAddsWorkflowDeliveryPlugin() {
        var service = new ObservabilityService();
        var trace = service.newTrace("sess-1");

        var withWorkflow = service.withWorkflowRun(trace, "wf-1");
        assertEquals("wf-1", withWorkflow.workflowRunId());

        var withDelivery = service.withDeliveryJob(withWorkflow, "job-1");
        assertEquals("job-1", withDelivery.deliveryJobId());

        var withPlugin = service.withPlugin(withDelivery, "plugin-1");
        assertEquals("plugin-1", withPlugin.pluginId());

        CorrelationContext.clear();
    }

    @Test
    void structuredLoggerIncludesCorrelationInOutput() {
        var logger = new StructuredLogger("test-component");
        CorrelationContext.set("correlationId", "corr-test");
        CorrelationContext.set("sessionId", "sess-test");

        assertDoesNotThrow(() -> logger.info("test message"));
        assertDoesNotThrow(() -> logger.warn("warning message"));
        assertDoesNotThrow(() -> logger.error("error message"));

        CorrelationContext.clear();
    }

    @Test
    void metricsCollectorInterfaceAcceptsImplementations() {
        AtomicInteger counter = new AtomicInteger();
        MetricsCollector collector = new MetricsCollector() {
            @Override public void incrementCounter(String name, String... tags) { counter.incrementAndGet(); }
            @Override public void recordHistogram(String name, double value, String... tags) {}
            @Override public void setGauge(String name, double value, String... tags) {}
        };

        collector.incrementCounter("test");
        collector.recordHistogram("test", 1.0);
        collector.setGauge("test", 1.0);

        assertEquals(1, counter.get());
    }
}
