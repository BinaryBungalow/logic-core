package ai.binbun.delivery.webhook;

public final class WebhookCallbackVerificationService {
    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier();

    public boolean verify(String providedSignature, String expectedSignature) {
        return verifier.verify(providedSignature, expectedSignature);
    }
}
