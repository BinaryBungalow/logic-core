package ai.binbun.integration;

import ai.binbun.delivery.core.ChannelConnector;
import ai.binbun.delivery.core.ConnectorCapability;
import ai.binbun.delivery.core.ConnectorRegistry;
import ai.binbun.delivery.core.DeliveryHealth;
import ai.binbun.delivery.core.DeliveryReceipt;
import ai.binbun.delivery.core.DeliveryService;
import ai.binbun.delivery.core.InboundMessageEnvelope;
import ai.binbun.delivery.core.OutboundMessage;
import ai.binbun.delivery.model.DeliveryJobStatus;
import ai.binbun.delivery.model.JsonDeliveryJobRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Phase2DeliveryFailurePersistenceTest {
    @Test
    void persistsFailedOutboundDeliveryState() throws Exception {
        var temp = Files.createTempDirectory("binbun-phase2-delivery-failure");
        var connectors = new ConnectorRegistry();
        connectors.register(new FailingConnector());
        var repository = new JsonDeliveryJobRepository(temp);
        var service = new DeliveryService(connectors, repository);

        assertThrows(IllegalStateException.class, () ->
                service.send("failing", new OutboundMessage("idem-fail-1", "session-2", "dest-2", "boom", Map.of("kind", "failure-test"), null))
        );

        var stored = repository.findByIdempotencyKey("failing", "idem-fail-1").orElseThrow();
        assertEquals(DeliveryJobStatus.FAILED, stored.status());
        assertEquals(1, stored.retryCount());
        assertEquals("connector failure", stored.lastError());
        assertEquals("session-2", stored.sessionId());
    }

    private static final class FailingConnector implements ChannelConnector {
        @Override
        public String name() {
            return "failing";
        }

        @Override
        public Set<ConnectorCapability> capabilities() {
            return Set.of(ConnectorCapability.OUTBOUND_TEXT);
        }

        @Override
        public DeliveryHealth health() {
            return DeliveryHealth.degraded("intentional test failure connector");
        }

        @Override
        public DeliveryReceipt send(OutboundMessage message) {
            throw new IllegalStateException("connector failure");
        }

        @Override
        public InboundMessageEnvelope normalizeInbound(String payload) {
            return new InboundMessageEnvelope(name(), "test", payload, "msg-1", Map.of(), null);
        }

        @Override
        public String normalizeOutbound(OutboundMessage message) {
            return message.text();
        }
    }
}
