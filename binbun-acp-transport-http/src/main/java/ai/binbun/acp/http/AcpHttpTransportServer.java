package ai.binbun.acp.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ai.binbun.acp.protocol.AcpEnvelopes;
import ai.binbun.acp.protocol.AcpOperation;
import ai.binbun.acp.socket.AcpSocketConnectionState;
import ai.binbun.acp.socket.AcpSocketProtocolHandler;
import ai.binbun.delivery.webhook.WebhookCallbackVerificationService;
import ai.binbun.gateway.GatewayAgentSessionService;
import ai.binbun.gateway.GatewayInboundDeliveryHandler;
import ai.binbun.gateway.GatewayRuntime;
import ai.binbun.gateway.health.GatewayHealthService;
import ai.binbun.gateway.recovery.GatewayRecoveryCoordinator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class AcpHttpTransportServer implements AutoCloseable {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GatewayRuntime gatewayRuntime;
    private final GatewayAgentSessionService sessionService;
    private final AcpHttpProtocolAdapter protocolAdapter = new AcpHttpProtocolAdapter();
    private final AcpHttpEventBridge eventBridge;
    private final AcpSocketProtocolHandler protocolHandler;
    private final GatewayHealthService healthService;
    private final GatewayRecoveryCoordinator recoveryCoordinator;
    private final GatewayInboundDeliveryHandler inboundDeliveryHandler;
    private final Supplier<Object> operationalSnapshotSupplier;
    private final Supplier<Object> readinessSupplier;
    private final Supplier<Object> pluginStatusSupplier;
    private final Supplier<Object> deliveryStatusSupplier;
    private final WebhookCallbackVerificationService webhookVerification = new WebhookCallbackVerificationService();
    private final ConcurrentHashMap<String, AcpSocketConnectionState> clients = new ConcurrentHashMap<>();
    private final HttpServer server;

    public AcpHttpTransportServer(GatewayRuntime gatewayRuntime, GatewayAgentSessionService sessionService,
                                  AcpSocketProtocolHandler protocolHandler, GatewayHealthService healthService,
                                  GatewayRecoveryCoordinator recoveryCoordinator,
                                  GatewayInboundDeliveryHandler inboundDeliveryHandler, int port) {
        this(gatewayRuntime, sessionService, protocolHandler, healthService, recoveryCoordinator, inboundDeliveryHandler,
                () -> Map.of("available", false), () -> Map.of("liveness", "UP", "readiness", "UP", "degraded", false),
                () -> Map.of("available", false), () -> Map.of("available", false), port);
    }

    public AcpHttpTransportServer(GatewayRuntime gatewayRuntime, GatewayAgentSessionService sessionService,
                                  AcpSocketProtocolHandler protocolHandler, GatewayHealthService healthService,
                                  GatewayRecoveryCoordinator recoveryCoordinator,
                                  GatewayInboundDeliveryHandler inboundDeliveryHandler,
                                  Supplier<Object> operationalSnapshotSupplier,
                                  int port) {
        this(gatewayRuntime, sessionService, protocolHandler, healthService, recoveryCoordinator, inboundDeliveryHandler,
                operationalSnapshotSupplier, () -> Map.of("liveness", "UP", "readiness", "UP", "degraded", false),
                () -> Map.of("available", false), () -> Map.of("available", false), port);
    }

    public AcpHttpTransportServer(GatewayRuntime gatewayRuntime, GatewayAgentSessionService sessionService,
                                  AcpSocketProtocolHandler protocolHandler, GatewayHealthService healthService,
                                  GatewayRecoveryCoordinator recoveryCoordinator,
                                  GatewayInboundDeliveryHandler inboundDeliveryHandler,
                                  Supplier<Object> operationalSnapshotSupplier,
                                  Supplier<Object> pluginStatusSupplier,
                                  Supplier<Object> deliveryStatusSupplier,
                                  int port) {
        this(gatewayRuntime, sessionService, protocolHandler, healthService, recoveryCoordinator, inboundDeliveryHandler,
                operationalSnapshotSupplier, () -> Map.of("liveness", "UP", "readiness", "UP", "degraded", false),
                pluginStatusSupplier, deliveryStatusSupplier, port);
    }

    public AcpHttpTransportServer(GatewayRuntime gatewayRuntime, GatewayAgentSessionService sessionService,
                                  AcpSocketProtocolHandler protocolHandler, GatewayHealthService healthService,
                                  GatewayRecoveryCoordinator recoveryCoordinator,
                                  GatewayInboundDeliveryHandler inboundDeliveryHandler,
                                  Supplier<Object> operationalSnapshotSupplier,
                                  Supplier<Object> readinessSupplier,
                                  Supplier<Object> pluginStatusSupplier,
                                  Supplier<Object> deliveryStatusSupplier,
                                  int port) {
        try {
            this.gatewayRuntime = gatewayRuntime;
            this.sessionService = sessionService;
            this.protocolHandler = protocolHandler;
            this.healthService = healthService;
            this.recoveryCoordinator = recoveryCoordinator;
            this.inboundDeliveryHandler = inboundDeliveryHandler;
            this.operationalSnapshotSupplier = operationalSnapshotSupplier;
            this.readinessSupplier = readinessSupplier;
            this.pluginStatusSupplier = pluginStatusSupplier;
            this.deliveryStatusSupplier = deliveryStatusSupplier;
            this.eventBridge = new AcpHttpEventBridge(sessionService);
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            registerRoutes();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start ACP HTTP transport", e);
        }
    }

    public void start() {
        server.start();
    }

    private void registerRoutes() {
        server.createContext("/health", exchange -> writeJson(exchange, 200, Map.of("ok", true)));
        server.createContext("/gateway/health", exchange -> writeJson(exchange, 200, healthService.snapshot()));
        server.createContext("/gateway/recovery", exchange -> writeJson(exchange, 200, recoveryCoordinator.startupPlan()));
        server.createContext("/gateway/operational", exchange -> writeJson(exchange, 200, operationalSnapshotSupplier.get()));
        server.createContext("/gateway/readiness", exchange -> writeJson(exchange, 200, readinessSupplier.get()));
        server.createContext("/gateway/plugins", exchange -> writeJson(exchange, 200, pluginStatusSupplier.get()));
        server.createContext("/delivery/status", exchange -> writeJson(exchange, 200, deliveryStatusSupplier.get()));
        server.createContext("/acp/sessions", exchange -> writeJson(exchange, 200, gatewayRuntime.sessions()));
        server.createContext("/delivery/inbound", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, Map.of("error", "method_not_allowed"));
                return;
            }
            String connector = queryValue(exchange.getRequestURI().getQuery(), "connector");
            String sessionId = queryValue(exchange.getRequestURI().getQuery(), "sessionId");
            if (connector == null || sessionId == null) {
                writeJson(exchange, 400, Map.of("error", "missing_connector_or_session"));
                return;
            }
            if ("webhook".equals(connector)) {
                String expectedSignature = queryValue(exchange.getRequestURI().getQuery(), "expectedSignature");
                if (expectedSignature != null) {
                    String providedSignature = exchange.getRequestHeaders().getFirst("X-Webhook-Signature");
                    if (!webhookVerification.verify(providedSignature, expectedSignature)) {
                        writeJson(exchange, 401, Map.of("error", "invalid_webhook_signature"));
                        return;
                    }
                }
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var run = inboundDeliveryHandler.handle(connector, sessionId, body);
            writeJson(exchange, 202, Map.of("accepted", true, "runId", run.id(), "sessionId", sessionId));
        });
        server.createContext("/acp/protocol", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, Map.of("error", "method_not_allowed"));
                return;
            }
            String clientId = exchange.getRequestHeaders().getFirst("X-ACP-Client");
            if (clientId == null || clientId.isBlank()) {
                writeJson(exchange, 400, Map.of("error", "missing_client_id"));
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                var request = protocolAdapter.decode(body);
                var state = clients.computeIfAbsent(clientId, ignored -> new AcpSocketConnectionState());
                var response = protocolHandler.handle(request, state);
                byte[] payload = protocolAdapter.encode(response).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, payload.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(payload);
                }
            } catch (RuntimeException e) {
                var error = AcpEnvelopes.error(AcpOperation.ERROR, null, 0L, "malformed-frame", "bad_request", "Malformed ACP frame");
                byte[] payload = protocolAdapter.encode(error).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, payload.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(payload);
                }
            }
        });
        server.createContext("/acp/events", exchange -> {
            String clientId = exchange.getRequestHeaders().getFirst("X-ACP-Client");
            String sessionId = queryValue(exchange.getRequestURI().getQuery(), "sessionId");
            long lastAck = longValue(queryValue(exchange.getRequestURI().getQuery(), "lastAckSequence"));
            if (clientId == null || clientId.isBlank() || sessionId == null || sessionId.isBlank()) {
                writeJson(exchange, 400, Map.of("error", "missing_client_or_session"));
                return;
            }
            sessionService.find(sessionId).orElseThrow(() -> new IllegalArgumentException("Unknown session: " + sessionId));
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                var replay = eventBridge.replaySince(sessionId, lastAck);
                if (replay.isEmpty()) {
                    os.write(("event: heartbeat\n" + "data: {\"sessionId\":\"" + sessionId + "\", \"lastAckSequence\":" + lastAck + "}\n\n").getBytes(StandardCharsets.UTF_8));
                } else {
                    for (var envelope : replay) {
                        os.write(("event: acp\n" + "data: " + protocolAdapter.encode(envelope) + "\n\n").getBytes(StandardCharsets.UTF_8));
                    }
                }
                os.flush();
            }
        });
    }

    private String queryValue(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals(key)) {
                return pair[1];
            }
        }
        return null;
    }

    private long longValue(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] json = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, json.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
