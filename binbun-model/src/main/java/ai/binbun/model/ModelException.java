package ai.binbun.model;

public final class ModelException extends RuntimeException {
    private final ProviderKind provider;

    public ModelException(ProviderKind provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public ProviderKind provider() {
        return provider;
    }
}
