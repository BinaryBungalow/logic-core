package ai.binbun.acp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class AcpHttpServer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AcpGateway gateway;
    private final HttpServer server;

    public AcpHttpServer(AcpGateway gateway, int port) {
        try {
            this.gateway = gateway;
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            registerRoutes();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start ACP HTTP server", e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void registerRoutes() {
        server.createContext("/health", exchange -> writeJson(exchange, 200, Map.of("ok", true)));
        server.createContext("/acp/sessions", exchange -> writeJson(exchange, 200, gateway.sessions()));
        server.createContext("/acp/session", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) {
                writeJson(exchange, 400, Map.of("error", "missing session id"));
                return;
            }
            String sessionId = parts[3];
            try {
                writeJson(exchange, 200, gateway.attachAcpSession(sessionId));
            } catch (IllegalArgumentException e) {
                writeJson(exchange, 404, Map.of("error", e.getMessage()));
            }
        });
        server.createContext("/acp/events", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);
            AtomicReference<AcpGateway.AcpEvent> lastEvent = new AtomicReference<>(new AcpGateway.AcpEvent("heartbeat", "ready"));
            CountDownLatch latch = new CountDownLatch(1);
            gateway.subscribeEvents(event -> {
                lastEvent.set(event);
                latch.countDown();
            });
            try {
                latch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String payload = "event: " + lastEvent.get().type() + "\n" +
                    "data: " + objectMapper.writeValueAsString(lastEvent.get()) + "\n\n";
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });
    }

    private void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] json = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, json.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json);
        }
    }
}
