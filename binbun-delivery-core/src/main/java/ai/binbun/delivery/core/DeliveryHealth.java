package ai.binbun.delivery.core;

public record DeliveryHealth(boolean healthy, String status, String detail) {
    public static DeliveryHealth healthy(String detail) {
        return new DeliveryHealth(true, "UP", detail);
    }

    public static DeliveryHealth degraded(String detail) {
        return new DeliveryHealth(false, "DEGRADED", detail);
    }
}
