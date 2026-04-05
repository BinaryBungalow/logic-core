package ai.binbun.nativetools;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class WebhookMessageDispatcher implements MessageDispatcher {
    private final URI webhookUri;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebhookMessageDispatcher(String webhookUrl) {
        this.webhookUri = URI.create(webhookUrl);
    }

    @Override
    public String dispatch(MessageDispatch dispatch) {
        try {
            HttpURLConnection connection = (HttpURLConnection) webhookUri.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] payload = objectMapper.writeValueAsString(Map.of(
                    "destination", dispatch.destination(),
                    "body", dispatch.body(),
                    "channel", dispatch.channel()
            )).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload);
            }
            int code = connection.getResponseCode();
            return "queued:webhook:" + code + ":" + dispatch.destination();
        } catch (IOException e) {
            throw new IllegalStateException("Webhook dispatch failed", e);
        }
    }
}
