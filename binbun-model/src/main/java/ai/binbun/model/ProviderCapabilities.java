package ai.binbun.model;

public record ProviderCapabilities(boolean supportsStreaming, boolean supportsTools, boolean supportsSystemMessages) {
}
