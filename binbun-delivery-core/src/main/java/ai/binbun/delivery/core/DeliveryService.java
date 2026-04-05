package ai.binbun.delivery.core;

import ai.binbun.delivery.model.DeliveryJob;
import ai.binbun.delivery.model.DeliveryJobRepository;
import ai.binbun.delivery.model.DeliveryJobStatus;

import java.time.Instant;

public final class DeliveryService {
    private final ConnectorRegistry connectors;
    private final DeliveryJobRepository repository;

    public DeliveryService(ConnectorRegistry connectors, DeliveryJobRepository repository) {
        this.connectors = connectors;
        this.repository = repository;
    }

    public DeliveryReceipt send(String connectorName, OutboundMessage message) {
        var existing = repository.findByIdempotencyKey(connectorName, message.idempotencyKey());
        if (existing.isPresent() && existing.get().status() == DeliveryJobStatus.SENT) {
            return new DeliveryReceipt(connectorName, existing.get().providerMessageId(), existing.get().status().name());
        }
        var connector = connectors.find(connectorName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown connector: " + connectorName));
        repository.save(new DeliveryJob(connectorName, message.idempotencyKey(), message.sessionId(), message.destination(),
                connector.normalizeOutbound(message), DeliveryJobStatus.PENDING, 0, null, null, Instant.now(), Instant.now()));
        try {
            var receipt = connector.send(message);
            repository.save(new DeliveryJob(connectorName, message.idempotencyKey(), message.sessionId(), message.destination(),
                    connector.normalizeOutbound(message), DeliveryJobStatus.SENT, 0, null, receipt.providerMessageId(), Instant.now(), Instant.now()));
            return receipt;
        } catch (RuntimeException e) {
            repository.save(new DeliveryJob(connectorName, message.idempotencyKey(), message.sessionId(), message.destination(),
                    connector.normalizeOutbound(message), DeliveryJobStatus.FAILED, 1, e.getMessage(), null, Instant.now(), Instant.now()));
            throw e;
        }
    }

    public DeliveryManagedResult sendManaged(String connectorName, OutboundMessage message, DeliveryFailureHandler failureHandler) {
        try {
            var receipt = send(connectorName, message);
            return new DeliveryManagedResult(true, receipt, null, null);
        } catch (RuntimeException e) {
            var decision = failureHandler.onFailure(connectorName, message.idempotencyKey(), message.sessionId(), e.getMessage(), 1);
            return new DeliveryManagedResult(false, null, decision, e.getMessage());
        }
    }
}
