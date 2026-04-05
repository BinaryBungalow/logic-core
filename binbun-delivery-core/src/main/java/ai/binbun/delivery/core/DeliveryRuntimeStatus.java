package ai.binbun.delivery.core;

public record DeliveryRuntimeStatus(
        int totalJobs,
        int pendingJobs,
        int sentJobs,
        int failedJobs,
        int deadLetterCount,
        boolean retryEnabled
) {
}
