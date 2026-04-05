package ai.binbun.delivery.webhook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.delivery.core.ChannelConnector;
import ai.binbun.delivery.core.ConnectorCapability;
import ai.binbun.delivery.core.DeliveryHealth;
import ai.binbun.delivery.core.DeliveryReceipt;
import ai.binbun.delivery.core.InboundMessageEnvelope;
import ai.binbun.delivery.core.OutboundMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WebhookChannelConnector implements ChannelConnector {
    private final URI endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebhookChannelConnector(String endpoint) {
        this.endpoint = URI.create(endpoint);
    }

    @Override
    public String name() {
        return "webhook";
    }

    @Override
    public Set<ConnectorCapability> capabilities() {
        return Set.of(ConnectorCapability.OUTBOUND_TEXT, ConnectorCapability.INBOUND_EVENTS);
    }

    @Override
    public DeliveryHealth health() {
        return DeliveryHealth.healthy("webhook endpoint configured");
    }

    @Override
    public DeliveryReceipt send(OutboundMessage message) {
        try {
            HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] payload = normalizeOutbound(message).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload);
            }
            int code = connection.getResponseCode();
            return new DeliveryReceipt(name(), "webhook-" + code + "-" + UUID.randomUUID(), code < 400 ? "SENT" : "FAILED");
        } catch (IOException e) {
            throw new IllegalStateException("Webhook delivery failed", e);
        }
    }

    @Override
    public InboundMessageEnvelope normalizeInbound(String payload) {
        try {
            Map<String, Object> body = objectMapper.readValue(payload, new TypeReference<>() {});
            return new InboundMessageEnvelope(
                    name(),
                    String.valueOf(body.getOrDefault("source", "webhook")),
                    String.valueOf(body.getOrDefault("text", "")),
                    String.valueOf(body.getOrDefault("providerMessageId", UUID.randomUUID().toString())),
                    body,
                    null
            );
        } catch (IOException e) {
            return new InboundMessageEnvelope(name(), "webhook", payload, UUID.randomUUID().toString(), Map.of("raw", payload), null);
        }
    }

    @Override
    public String normalizeOutbound(OutboundMessage message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "sessionId", message.sessionId(),
                    "destination", message.destination(),
                    "text", message.text(),
                    "metadata", message.metadata()
            ));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to encode webhook payload", e);
        }
    }
}
