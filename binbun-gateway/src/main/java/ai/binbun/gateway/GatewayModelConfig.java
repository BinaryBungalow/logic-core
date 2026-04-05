package ai.binbun.gateway;

import ai.binbun.model.ProviderKind;

import java.net.URI;

public record GatewayModelConfig(ProviderKind provider, URI baseUri, String apiKey, String model) {
}
