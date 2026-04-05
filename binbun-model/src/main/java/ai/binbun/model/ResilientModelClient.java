package ai.binbun.model;

import java.util.concurrent.Flow;

public final class ResilientModelClient implements ModelClient {
    private final ModelClient delegate;
    private final ProviderKind provider;
    private final RetryPolicy retryPolicy;

    public ResilientModelClient(ModelClient delegate, ProviderKind provider, RetryPolicy retryPolicy) {
        this.delegate = delegate;
        this.provider = provider;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public ChatResponse complete(ChatRequest request) {
        RuntimeException last = null;
        long delay = retryPolicy.initialDelay().toMillis();
        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            try {
                return delegate.complete(request);
            } catch (RuntimeException e) {
                last = normalize(e, attempt);
                if (attempt == retryPolicy.maxAttempts()) {
                    throw last;
                }
                sleep(delay);
                delay = Math.max(delay, (long) (delay * retryPolicy.backoffMultiplier()));
            }
        }
        throw last == null ? new ModelException(provider, "Unknown model failure", null) : last;
    }

    @Override
    public Flow.Publisher<StreamingChunk> stream(ChatRequest request) {
        try {
            return delegate.stream(request);
        } catch (RuntimeException e) {
            throw normalize(e, 1);
        }
    }

    @Override
    public Flow.Publisher<StreamingChunk> stream(ChatRequest request, StreamObserver observer) {
        try {
            return delegate.stream(request, observer);
        } catch (RuntimeException e) {
            throw normalize(e, 1);
        }
    }

    private RuntimeException normalize(RuntimeException error, int attempt) {
        if (error instanceof ModelException modelException) {
            return modelException;
        }
        String message = "Provider " + provider.name().toLowerCase() + " failed on attempt " + attempt + ": " +
                (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        return new ModelException(provider, message, error);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException(provider, "Interrupted during retry backoff", e);
        }
    }
}
