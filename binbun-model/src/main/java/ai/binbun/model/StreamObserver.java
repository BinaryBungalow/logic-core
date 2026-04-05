package ai.binbun.model;

public interface StreamObserver {
    default void onRawLine(String line) {
    }

    default void onChunk(StreamingChunk chunk) {
    }
}
