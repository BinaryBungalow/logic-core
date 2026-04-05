package ai.binbun.integration;

import ai.binbun.delivery.webhook.WebhookSignatureVerifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2WebhookSignatureTest {
    @Test
    void rejectsInvalidWebhookSignatures() {
        var verifier = new WebhookSignatureVerifier();
        assertTrue(verifier.verify("sig-1", "sig-1"));
        assertFalse(verifier.verify("sig-1", "sig-2"));
        assertFalse(verifier.verify(null, "sig-2"));
    }
}
