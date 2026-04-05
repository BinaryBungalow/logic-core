package ai.binbun.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RequestValidatorTest {
    @Test
    void allowsSupportedRequestShapes() {
        var request = new ChatRequest("m", List.of(new ChatRequest.Message("system", "s")), true, List.of());
        var profile = new ProviderProfile(ProviderKind.OPENAI, "https://api.openai.com", "gpt-4.1-mini", new ProviderCapabilities(true, true, true));
        assertDoesNotThrow(() -> RequestValidator.validate(request, profile));
    }
}
