package ai.binbun.integration;

import ai.binbun.delivery.core.ChannelConnector;
import ai.binbun.delivery.core.ConnectorCapability;
import ai.binbun.delivery.core.ConnectorRegistry;
import ai.binbun.delivery.core.DeliveryFailureHandler;
import ai.binbun.delivery.core.DeliveryHealth;
import ai.binbun.delivery.core.DeliveryManagedResult;
import ai.binbun.delivery.core.DeliveryReceipt;
import ai.binbun.delivery.core.DeliveryRetryPlanner;
import ai.binbun.delivery.core.DeliveryRetryPolicy;
import ai.binbun.delivery.core.DeliveryService;
import ai.binbun.delivery.core.InboundMessageEnvelope;
import ai.binbun.delivery.core.OutboundMessage;
import ai.binbun.delivery.model.JsonDeliveryDeadLetterRepository;
import ai.binbun.delivery.model.JsonDeliveryJobRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2ManagedDeliveryTest {
    @Test
    void managedDeliveryTurnsRealSendFailureIntoDecisionAndDeadLetter() throws Exception {
        var connectors = new ConnectorRegistry();
        connectors.register(new FailingConnector());
        var service = new DeliveryService(connectors, new JsonDeliveryJobRepository(Files.createTempDirectory("binbun-managed-delivery-jobs")));
        var failureHandler = new DeliveryFailureHandler(new DeliveryRetryPlanner(new DeliveryRetryPolicy(1, 500)), new JsonDeliveryDeadLetterRepository(Files.createTempDirectory("binbun-managed-delivery-dead")));
        DeliveryManagedResult result = service.sendManaged("failing", new OutboundMessage("idem-managed", "session-managed", "dest", "hello", Map.of(), null), failureHandler);
        assertFalse(result.sent());
        assertFalse(result.failureDecision().retryable());
        assertTrue(result.failureDecision().deadLettered());
    }

    private static final class FailingConnector implements ChannelConnector {
        @Override public String name() { return "failing"; }
        @Override public Set<ConnectorCapability> capabilities() { return Set.of(ConnectorCapability.OUTBOUND_TEXT); }
        @Override public DeliveryHealth health() { return DeliveryHealth.degraded("failing test connector"); }
        @Override public DeliveryReceipt send(OutboundMessage message) { throw new IllegalStateException("connector failure"); }
        @Override public InboundMessageEnvelope normalizeInbound(String payload) { return new InboundMessageEnvelope(name(), "test", payload, "msg", Map.of(), null); }
        @Override public String normalizeOutbound(OutboundMessage message) { return message.text(); }
    }
}
