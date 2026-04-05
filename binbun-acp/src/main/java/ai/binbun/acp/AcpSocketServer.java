package ai.binbun.acp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.agent.event.AgentEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AcpSocketServer implements AutoCloseable {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AcpSessionService sessionService;
    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public AcpSocketServer(AcpSessionService sessionService, int port) {
        this.sessionService = sessionService;
        this.port = port;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to bind ACP socket server", e);
        }
        acceptThread = Thread.ofVirtual().start(() -> {
            while (running.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    Thread.ofVirtual().start(() -> handle(socket));
                } catch (IOException e) {
                    if (running.get()) {
                        throw new IllegalStateException("ACP accept failed", e);
                    }
                }
            }
        });
    }

    private void handle(Socket socket) {
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode request = objectMapper.readTree(line);
                String op = request.path("op").asText("");
                switch (op) {
                    case "attach" -> attach(request, writer);
                    case "prompt" -> prompt(request, writer);
                    case "close" -> close(request, writer);
                    default -> write(writer, Map.of("type", "error", "message", "Unknown op: " + op));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("ACP socket handler failed", e);
        }
    }

    private void attach(JsonNode request, BufferedWriter writer) throws IOException {
        String sessionId = request.path("sessionId").asText("");
        var managed = sessionService.find(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown session: " + sessionId));
        managed.events().subscribe(new Flow.Subscriber<AgentEvent>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(AgentEvent item) {
                synchronized (writer) {
                    try {
                        write(writer, Map.of("type", "event", "sessionId", sessionId, "event", item));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }

            @Override public void onError(Throwable throwable) {}
            @Override public void onComplete() {}
        });
        write(writer, Map.of("type", "attached", "sessionId", sessionId));
    }

    private void prompt(JsonNode request, BufferedWriter writer) throws IOException {
        String sessionId = request.path("sessionId").asText("");
        String input = request.path("input").asText("");
        var run = sessionService.promptAcp(sessionId, input);
        write(writer, Map.of("type", "run", "sessionId", sessionId, "runId", run.id()));
    }

    private void close(JsonNode request, BufferedWriter writer) throws IOException {
        String sessionId = request.path("sessionId").asText("");
        sessionService.close(sessionId);
        write(writer, Map.of("type", "closed", "sessionId", sessionId));
    }

    private void write(BufferedWriter writer, Object payload) throws IOException {
        writer.write(objectMapper.writeValueAsString(payload));
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() {
        running.set(false);
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
