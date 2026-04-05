package ai.binbun.delivery.core;

public final class DeliveryRetryLifecycleService {
    private final DeliveryFailureHandler failureHandler;

    public DeliveryRetryLifecycleService(DeliveryFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
    }

    public DeliveryRetryLifecycleRecord evaluate(String connector, String idempotencyKey, String sessionId, int attemptsSoFar, String error) {
        var decision = failureHandler.onFailure(connector, idempotencyKey, sessionId, error, attemptsSoFar);
        return new DeliveryRetryLifecycleRecord(
                connector,
                idempotencyKey,
                sessionId,
                attemptsSoFar,
                decision.retryable(),
                decision.nextDelayMillis(),
                decision.deadLettered(),
                error
        );
    }
}
