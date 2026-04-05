package ai.binbun.delivery.core;

public record DeliveryReceipt(String connector, String providerMessageId, String status) {
}
