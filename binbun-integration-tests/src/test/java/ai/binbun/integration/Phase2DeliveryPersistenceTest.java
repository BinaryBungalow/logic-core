package ai.binbun.integration;

import ai.binbun.delivery.core.ConnectorRegistry;
import ai.binbun.delivery.core.DeliveryService;
import ai.binbun.delivery.core.OutboundMessage;
import ai.binbun.delivery.model.JsonDeliveryJobRepository;
import ai.binbun.delivery.model.DeliveryJobStatus;
import ai.binbun.delivery.telegram.TelegramChannelConnector;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Phase2DeliveryPersistenceTest {
    @Test
    void persistsOutboundDeliveryAndHonorsIdempotencyKey() throws Exception {
        var temp = Files.createTempDirectory("binbun-phase2-delivery-store");
        var connectors = new ConnectorRegistry();
        connectors.register(new TelegramChannelConnector("test-bot-token"));
        var repository = new JsonDeliveryJobRepository(temp);
        var service = new DeliveryService(connectors, repository);

        var first = service.send("telegram", new OutboundMessage("idem-1", "session-1", "chat-1", "hello", Map.of("kind", "test"), null));
        var second = service.send("telegram", new OutboundMessage("idem-1", "session-1", "chat-1", "hello", Map.of("kind", "test"), null));
        var stored = repository.findByIdempotencyKey("telegram", "idem-1").orElseThrow();

        assertEquals(first.providerMessageId(), second.providerMessageId());
        assertEquals(1, repository.list().size());
        assertEquals(DeliveryJobStatus.SENT, stored.status());
        assertEquals("session-1", stored.sessionId());
    }
}
