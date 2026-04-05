package ai.binbun.model;

import java.util.concurrent.Flow;

public interface ModelClient {
    ChatResponse complete(ChatRequest request);

    Flow.Publisher<StreamingChunk> stream(ChatRequest request);

    default Flow.Publisher<StreamingChunk> stream(ChatRequest request, StreamObserver observer) {
        return stream(request);
    }
}
