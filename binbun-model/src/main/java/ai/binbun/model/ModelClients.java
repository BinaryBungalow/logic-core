package ai.binbun.model;

import java.net.URI;

public final class ModelClients {
    private ModelClients() {
    }

    public static ModelClient create(ProviderKind providerKind, URI baseUri, String apiKey) {
        ProviderProfile profile = profile(providerKind);
        ModelClient base = switch (providerKind) {
            case ANTHROPIC -> new AnthropicMessagesModelClient(baseUri, apiKey);
            case GOOGLE -> new GoogleGenAiModelClient(baseUri, apiKey);
            case OPENAI -> new OpenAiCompatibleModelClient(baseUri, apiKey);
        };
        return new ResilientModelClient(new ValidatingModelClient(base, profile), providerKind, RetryPolicy.defaults());
    }

    public static ProviderProfile profile(ProviderKind providerKind) {
        return switch (providerKind) {
            case ANTHROPIC -> new ProviderProfile(providerKind, "https://api.anthropic.com", "claude-sonnet-4-0",
                    new ProviderCapabilities(true, true, true));
            case GOOGLE -> new ProviderProfile(providerKind, "https://generativelanguage.googleapis.com", "gemini-2.5-flash",
                    new ProviderCapabilities(true, true, true));
            case OPENAI -> new ProviderProfile(providerKind, "https://api.openai.com", "gpt-4.1-mini",
                    new ProviderCapabilities(true, true, true));
        };
    }

    private static final class ValidatingModelClient implements ModelClient {
        private final ModelClient delegate;
        private final ProviderProfile profile;

        private ValidatingModelClient(ModelClient delegate, ProviderProfile profile) {
            this.delegate = delegate;
            this.profile = profile;
        }

        @Override
        public ChatResponse complete(ChatRequest request) {
            RequestValidator.validate(request, profile);
            return delegate.complete(request);
        }

        @Override
        public java.util.concurrent.Flow.Publisher<StreamingChunk> stream(ChatRequest request) {
            RequestValidator.validate(request, profile);
            return delegate.stream(request);
        }

        @Override
        public java.util.concurrent.Flow.Publisher<StreamingChunk> stream(ChatRequest request, StreamObserver observer) {
            RequestValidator.validate(request, profile);
            return delegate.stream(request, observer);
        }
    }
}
