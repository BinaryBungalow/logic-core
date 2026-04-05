package ai.binbun.integration;

import ai.binbun.delivery.core.*;
import ai.binbun.delivery.model.JsonDeliveryDeadLetterRepository;
import ai.binbun.delivery.model.JsonDeliveryJobRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Phase2DeliveryRetryTest {
    @Test
    void retryPlannerChecksShouldRetry() {
        var policy = new DeliveryRetryPolicy(3, 1000);
        var planner = new DeliveryRetryPlanner(policy);

        assertTrue(planner.shouldRetry(0));
        assertTrue(planner.shouldRetry(1));
        assertTrue(planner.shouldRetry(2));
        assertFalse(planner.shouldRetry(3));
        assertFalse(planner.shouldRetry(5));
    }

    @Test
    void retryPlannerCalculatesExponentialBackoff() {
        var policy = new DeliveryRetryPolicy(3, 1000);
        var planner = new DeliveryRetryPlanner(policy);

        assertEquals(1000, planner.nextDelayMillis(0));
        assertEquals(2000, planner.nextDelayMillis(1));
        assertEquals(3000, planner.nextDelayMillis(2));
    }

    @Test
    void retryExecutorCanBeStartedAndStopped() throws Exception {
        var connectors = new ConnectorRegistry();
        connectors.register(new TestConnector());
        var policy = new DeliveryRetryPolicy(2, 100);
        var failureHandler = new DeliveryFailureHandler(
                new DeliveryRetryPlanner(policy),
                new JsonDeliveryDeadLetterRepository(
                        java.nio.file.Files.createTempDirectory("binbun-dead-test")
                )
        );
        var retryExecutor = new DeliveryRetryExecutor(
                new JsonDeliveryJobRepository(
                        java.nio.file.Files.createTempDirectory("binbun-jobs-test")
                ),
                connectors, policy, failureHandler
        );

        retryExecutor.start();
        Thread.sleep(50);
        retryExecutor.close();
    }

    private static class TestConnector implements ChannelConnector {
        @Override public String name() { return "test"; }
        @Override public java.util.Set<ConnectorCapability> capabilities() { return java.util.Set.of(ConnectorCapability.OUTBOUND_TEXT); }
        @Override public DeliveryHealth health() { return DeliveryHealth.healthy("test"); }
        @Override public DeliveryReceipt send(OutboundMessage message) { return new DeliveryReceipt(name(), "test-1", "SENT"); }
        @Override public InboundMessageEnvelope normalizeInbound(String payload) { return new InboundMessageEnvelope(name(), "test", payload, "1", Map.of(), null); }
        @Override public String normalizeOutbound(OutboundMessage message) { return message.text(); }
    }
}
