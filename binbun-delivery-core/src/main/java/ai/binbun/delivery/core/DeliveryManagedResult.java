package ai.binbun.delivery.core;

public record DeliveryManagedResult(
        boolean sent,
        DeliveryReceipt receipt,
        DeliveryFailureDecision failureDecision,
        String error
) {
}
