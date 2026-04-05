package ai.binbun.gateway;

import ai.binbun.model.ModelClient;
import ai.binbun.model.ModelClients;
import ai.binbun.model.ProviderKind;

import java.net.URI;
import java.util.Locale;

public final class GatewayModelFactory {
    public GatewayModelConfig resolveFromEnvironment() {
        ProviderKind provider = ProviderKind.valueOf(System.getenv().getOrDefault("BINBUN_PROVIDER", "OPENAI").toUpperCase(Locale.ROOT));
        var profile = ModelClients.profile(provider);
        String base = System.getenv().getOrDefault("BINBUN_BASE_URL", profile.defaultBaseUrl());
        String model = System.getenv().getOrDefault("BINBUN_MODEL", profile.defaultModel());
        String apiKey = System.getenv().getOrDefault("BINBUN_API_KEY", "test-key");
        return new GatewayModelConfig(provider, URI.create(base), apiKey, model);
    }

    public ModelClient create(GatewayModelConfig config) {
        return ModelClients.create(config.provider(), config.baseUri(), config.apiKey());
    }
}
