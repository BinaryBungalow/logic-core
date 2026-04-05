package ai.binbun.model;

public record ProviderProfile(ProviderKind kind, String defaultBaseUrl, String defaultModel, ProviderCapabilities capabilities) {
}
