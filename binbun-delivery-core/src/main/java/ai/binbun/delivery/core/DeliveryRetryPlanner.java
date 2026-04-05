package ai.binbun.delivery.core;

public final class DeliveryRetryPlanner {
    private final DeliveryRetryPolicy policy;

    public DeliveryRetryPlanner(DeliveryRetryPolicy policy) {
        this.policy = policy;
    }

    public boolean shouldRetry(int attemptsSoFar) {
        return attemptsSoFar < policy.maxAttempts();
    }

    public long nextDelayMillis(int attemptsSoFar) {
        return policy.baseDelayMillis() * Math.max(1, attemptsSoFar + 1L);
    }
}
