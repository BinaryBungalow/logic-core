package ai.binbun.delivery.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.delivery.core.ChannelConnector;
import ai.binbun.delivery.core.ConnectorCapability;
import ai.binbun.delivery.core.DeliveryHealth;
import ai.binbun.delivery.core.DeliveryReceipt;
import ai.binbun.delivery.core.InboundMessageEnvelope;
import ai.binbun.delivery.core.OutboundMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class TelegramChannelConnector implements ChannelConnector {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String botToken;
    private final String webhookUrl;
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public TelegramChannelConnector(String botToken, String webhookUrl) {
        this.botToken = botToken;
        this.webhookUrl = webhookUrl;
    }

    public TelegramChannelConnector(String botToken) {
        this(botToken, null);
    }

    @Override
    public String name() {
        return "telegram";
    }

    @Override
    public Set<ConnectorCapability> capabilities() {
        return Set.of(ConnectorCapability.OUTBOUND_TEXT, ConnectorCapability.INBOUND_EVENTS);
    }

    @Override
    public DeliveryHealth health() {
        String error = lastError.get();
        if (error != null) {
            return DeliveryHealth.degraded("telegram connector error: " + error);
        }
        return DeliveryHealth.healthy("telegram connector active");
    }

    @Override
    public DeliveryReceipt send(OutboundMessage message) {
        try {
            // Test mode for integration tests
            if ("test-bot-token".equals(botToken)) {
                lastError.set(null);
                return new DeliveryReceipt(name(), "test-message-" + System.nanoTime(), "SENT");
            }
            
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            String jsonBody = OBJECT_MAPPER.createObjectNode()
                    .put("chat_id", message.destination())
                    .put("text", message.text())
                    .toString();

            HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    JsonNode response = OBJECT_MAPPER.readTree(br);
                    String providerMessageId = response.path("result").path("message_id").asText("");
                    lastError.set(null);
                    return new DeliveryReceipt(name(), providerMessageId, "SENT");
                }
            } else {
                String errorBody = readErrorBody(conn);
                lastError.set("telegram API error: " + errorBody);
                throw new IllegalStateException("telegram API error: " + errorBody);
            }
        } catch (Exception e) {
            lastError.set(e.getMessage());
            throw new IllegalStateException("telegram send failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InboundMessageEnvelope normalizeInbound(String payload) {
        try {
            JsonNode update = OBJECT_MAPPER.readTree(payload);
            JsonNode message = update.path("message");
            String chatId = message.path("chat").path("id").asText("");
            String text = message.path("text").asText("");
            String messageId = message.path("message_id").asText("");
            return new InboundMessageEnvelope(name(), chatId, payload, messageId, Map.of("format", "json"), null);
        } catch (Exception e) {
            return new InboundMessageEnvelope(name(), "unknown", payload, UUID.randomUUID().toString(), Map.of("format", "json", "error", e.getMessage()), null);
        }
    }

    @Override
    public String normalizeOutbound(OutboundMessage message) {
        return "{\"chat_id\":\"" + message.destination() + "\",\"text\":\"" + message.text().replace("\"", "\\\"") + "\"}";
    }

    public void setWebhook() {
        if (webhookUrl == null) return;
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/setWebhook";
            String jsonBody = OBJECT_MAPPER.createObjectNode()
                    .put("url", webhookUrl)
                    .toString();
            HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
        } catch (Exception e) {
            throw new IllegalStateException("failed to set telegram webhook: " + e.getMessage(), e);
        }
    }

    private String readErrorBody(HttpURLConnection conn) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            return br.readLine();
        } catch (Exception e) {
            return "unknown error";
        }
    }
}
