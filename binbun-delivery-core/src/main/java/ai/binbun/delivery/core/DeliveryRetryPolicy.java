package ai.binbun.delivery.core;

public record DeliveryRetryPolicy(int maxAttempts, long baseDelayMillis) {
    public DeliveryRetryPolicy {
        maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
        baseDelayMillis = baseDelayMillis <= 0 ? 1000L : baseDelayMillis;
    }
}
