package ai.binbun.delivery.slack;

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

public final class SlackChannelConnector implements ChannelConnector {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String botToken;
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public SlackChannelConnector(String botToken) {
        this.botToken = botToken;
    }

    public SlackChannelConnector() {
        this(null);
    }

    @Override
    public String name() {
        return "slack";
    }

    @Override
    public Set<ConnectorCapability> capabilities() {
        return Set.of(ConnectorCapability.OUTBOUND_TEXT, ConnectorCapability.INBOUND_EVENTS);
    }

    @Override
    public DeliveryHealth health() {
        if (botToken == null) {
            return DeliveryHealth.degraded("slack connector not configured (no bot token)");
        }
        String error = lastError.get();
        if (error != null) {
            return DeliveryHealth.degraded("slack connector error: " + error);
        }
        return DeliveryHealth.healthy("slack connector active");
    }

    @Override
    public DeliveryReceipt send(OutboundMessage message) {
        if (botToken == null) {
            throw new IllegalStateException("slack connector not configured");
        }
        try {
            String url = "https://slack.com/api/chat.postMessage";
            String jsonBody = OBJECT_MAPPER.createObjectNode()
                    .put("channel", message.destination())
                    .put("text", message.text())
                    .toString();

            HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + botToken);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    JsonNode response = OBJECT_MAPPER.readTree(br);
                    if (response.path("ok").asBoolean(false)) {
                        String providerMessageId = response.path("ts").asText("");
                        lastError.set(null);
                        return new DeliveryReceipt(name(), providerMessageId, "SENT");
                    } else {
                        String slackError = response.path("error").asText("unknown");
                        lastError.set("slack API error: " + slackError);
                        throw new IllegalStateException("slack API error: " + slackError);
                    }
                }
            } else {
                String errorBody = readErrorBody(conn);
                lastError.set("slack HTTP error: " + errorBody);
                throw new IllegalStateException("slack HTTP error: " + errorBody);
            }
        } catch (Exception e) {
            lastError.set(e.getMessage());
            throw new IllegalStateException("slack send failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InboundMessageEnvelope normalizeInbound(String payload) {
        try {
            JsonNode event = OBJECT_MAPPER.readTree(payload);
            String channelId = event.path("event").path("channel").asText("");
            String text = event.path("event").path("text").asText("");
            String messageId = event.path("event").path("ts").asText("");
            return new InboundMessageEnvelope(name(), channelId, payload, messageId, Map.of("format", "json"), null);
        } catch (Exception e) {
            return new InboundMessageEnvelope(name(), "unknown", payload, UUID.randomUUID().toString(), Map.of("format", "json", "error", e.getMessage()), null);
        }
    }

    @Override
    public String normalizeOutbound(OutboundMessage message) {
        return "{\"channel\":\"" + message.destination() + "\",\"text\":\"" + message.text().replace("\"", "\\\"") + "\"}";
    }

    private String readErrorBody(HttpURLConnection conn) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            return br.readLine();
        } catch (Exception e) {
            return "unknown error";
        }
    }
}
