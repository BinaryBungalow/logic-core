package ai.binbun.delivery.webhook;

public final class WebhookSignatureVerifier {
    public boolean verify(String providedSignature, String expectedSignature) {
        if (providedSignature == null || expectedSignature == null) {
            return false;
        }
        return providedSignature.equals(expectedSignature);
    }
}
