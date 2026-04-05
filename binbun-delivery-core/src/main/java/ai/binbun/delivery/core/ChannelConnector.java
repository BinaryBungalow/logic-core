package ai.binbun.delivery.core;

import java.util.Set;

public interface ChannelConnector {
    String name();
    Set<ConnectorCapability> capabilities();
    DeliveryHealth health();
    DeliveryReceipt send(OutboundMessage message);
    InboundMessageEnvelope normalizeInbound(String payload);
    String normalizeOutbound(OutboundMessage message);
    default boolean supportsCallbackVerification() {
        return capabilities().contains(ConnectorCapability.SIGNATURE_VERIFICATION);
    }
    default boolean supportsMedia() {
        return capabilities().contains(ConnectorCapability.MEDIA);
    }
}
