package ai.binbun.model;

import java.time.Duration;

public record RetryPolicy(int maxAttempts, Duration initialDelay, double backoffMultiplier) {
    public static RetryPolicy defaults() {
        return new RetryPolicy(3, Duration.ofMillis(250), 2.0d);
    }
}
