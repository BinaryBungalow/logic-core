package ai.binbun.delivery.core;

import ai.binbun.delivery.model.DeliveryDeadLetterRecord;
import ai.binbun.delivery.model.JsonDeliveryDeadLetterRepository;

import java.time.Instant;

public final class DeliveryFailureHandler {
    private final DeliveryRetryPlanner retryPlanner;
    private final JsonDeliveryDeadLetterRepository deadLetters;

    public DeliveryFailureHandler(DeliveryRetryPlanner retryPlanner, JsonDeliveryDeadLetterRepository deadLetters) {
        this.retryPlanner = retryPlanner;
        this.deadLetters = deadLetters;
    }

    public DeliveryFailureDecision onFailure(String connector, String idempotencyKey, String sessionId, String reason, int attemptsSoFar) {
        if (retryPlanner.shouldRetry(attemptsSoFar)) {
            return new DeliveryFailureDecision(true, retryPlanner.nextDelayMillis(attemptsSoFar), false);
        }
        deadLetters.save(new DeliveryDeadLetterRecord(connector, idempotencyKey, sessionId, reason, Instant.now()));
        return new DeliveryFailureDecision(false, 0L, true);
    }
}
