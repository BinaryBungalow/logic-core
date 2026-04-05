package ai.binbun.delivery.core;

public record DeliveryRetryLifecycleRecord(
        String connector,
        String idempotencyKey,
        String sessionId,
        int attemptsSoFar,
        boolean retryable,
        long nextDelayMillis,
        boolean deadLettered,
        String error
) {
}
