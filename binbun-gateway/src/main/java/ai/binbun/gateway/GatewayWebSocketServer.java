package ai.binbun.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GatewayWebSocketServer implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GatewayMethodRegistry methodRegistry;
    private final SubscriptionRegistry subscriptionRegistry;
    private final GatewayEventBus eventBus;
    private final SessionRegistry sessionRegistry;
    private final int port;
    private final java.net.ServerSocket serverSocket;
    private final java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(false);
    private Thread acceptThread;
    private final java.util.Map<String, GatewayConnection> connections = new ConcurrentHashMap<>();

    public GatewayWebSocketServer(GatewayMethodRegistry methodRegistry,
                                  SubscriptionRegistry subscriptionRegistry,
                                  GatewayEventBus eventBus,
                                  SessionRegistry sessionRegistry,
                                  int port) {
        this.methodRegistry = methodRegistry;
        this.subscriptionRegistry = subscriptionRegistry;
        this.eventBus = eventBus;
        this.sessionRegistry = sessionRegistry;
        this.port = port;
        try {
            this.serverSocket = new java.net.ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to bind gateway WebSocket server on port " + port, e);
        }
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        acceptThread = Thread.ofVirtual().name("gateway-ws-accept").start(() -> {
            while (running.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    GatewayConnection conn = new GatewayConnection();
                    connections.put(conn.id(), conn);
                    Thread.ofVirtual().name("gateway-ws-" + conn.id()).start(() -> handleConnection(socket, conn));
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("Gateway WS accept error: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void handleConnection(Socket socket, GatewayConnection conn) {
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && conn.isConnected()) {
                Map<String, Object> frame;
                try {
                    frame = MAPPER.readValue(line, Map.class);
                } catch (Exception e) {
                    writeResponse(writer, null, false, Map.of("error", "malformed_frame", "detail", e.getMessage()));
                    continue;
                }
                String type = (String) frame.get("type");
                if ("req".equals(type)) {
                    handleRequest(frame, conn, writer);
                } else if ("sub".equals(type)) {
                    handleSubscribe(frame, conn, writer);
                } else if ("unsub".equals(type)) {
                    handleUnsubscribe(frame, conn, writer);
                } else {
                    writeResponse(writer, (String) frame.get("id"), false, Map.of("error", "unknown_frame_type"));
                }
            }
        } catch (IOException e) {
            // Connection closed
        } finally {
            subscriptionRegistry.unsubscribeAll(conn);
            connections.remove(conn.id());
            conn.disconnect();
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRequest(Map<String, Object> frame, GatewayConnection conn, BufferedWriter writer) throws IOException {
        String id = (String) frame.get("id");
        String method = (String) frame.get("method");
        Map<String, Object> params = (Map<String, Object>) frame.getOrDefault("params", Map.of());

        var methodOpt = methodRegistry.find(method);
        if (methodOpt.isEmpty()) {
            writeResponse(writer, id, false, Map.of("error", "method_not_found", "method", method));
            return;
        }

        GatewayMethod gwMethod = methodOpt.get();

        // Scope enforcement
        if (conn.isAuthenticated()) {
            for (OperatorScope required : gwMethod.requiredScopes()) {
                if (!conn.scopes().contains(required)) {
                    writeResponse(writer, id, false, Map.of("error", "insufficient_scope", "required", required.name()));
                    return;
                }
            }
        } else if (!gwMethod.requiredScopes().isEmpty()) {
            writeResponse(writer, id, false, Map.of("error", "authentication_required"));
            return;
        }

        try {
            Map<String, Object> result = gwMethod.handler().handle(params, conn);
            writeResponse(writer, id, true, result);
        } catch (Exception e) {
            writeResponse(writer, id, false, Map.of("error", "internal_error", "detail", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSubscribe(Map<String, Object> frame, GatewayConnection conn, BufferedWriter writer) throws IOException {
        String id = (String) frame.get("id");
        String eventType = (String) frame.get("event");
        subscriptionRegistry.subscribe(conn, eventType, payload -> {
            try {
                synchronized (writer) {
                    writer.write(MAPPER.writeValueAsString(Map.of(
                            "type", "event",
                            "event", eventType,
                            "payload", payload,
                            "seq", conn.nextSequence()
                    )));
                    writer.newLine();
                    writer.flush();
                }
            } catch (IOException e) {
                conn.disconnect();
            }
        });
        writeResponse(writer, id, true, Map.of("subscribed", eventType));
    }

    @SuppressWarnings("unchecked")
    private void handleUnsubscribe(Map<String, Object> frame, GatewayConnection conn, BufferedWriter writer) throws IOException {
        String id = (String) frame.get("id");
        String eventType = (String) frame.get("event");
        subscriptionRegistry.unsubscribe(conn, eventType);
        writeResponse(writer, id, true, Map.of("unsubscribed", eventType));
    }

    private void writeResponse(BufferedWriter writer, String id, boolean ok, Map<String, Object> payload) throws IOException {
        synchronized (writer) {
            writer.write(MAPPER.writeValueAsString(Map.of(
                    "type", "res",
                    "id", id,
                    "ok", ok,
                    "payload", payload
            )));
            writer.newLine();
            writer.flush();
        }
    }

    public int port() { return port; }

    public int connectionCount() { return connections.size(); }

    @Override
    public void close() {
        running.set(false);
        connections.values().forEach(GatewayConnection::disconnect);
        connections.clear();
        if (acceptThread != null) acceptThread.interrupt();
        try { serverSocket.close(); } catch (IOException ignored) {}
    }
}
