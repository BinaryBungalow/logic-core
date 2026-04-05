package ai.binbun.delivery.core;

public record DeliveryFailureDecision(boolean retryable, long nextDelayMillis, boolean deadLettered) {
}
