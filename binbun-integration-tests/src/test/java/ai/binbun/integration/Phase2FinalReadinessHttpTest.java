package ai.binbun.integration;

import ai.binbun.acp.AcpProtocolDescription;
import ai.binbun.acp.AcpServer;
import ai.binbun.acp.auth.AcpAuthService;
import ai.binbun.acp.auth.StaticTokenAuthenticator;
import ai.binbun.acp.http.AcpHttpTransportServer;
import ai.binbun.acp.socket.AcpSocketProtocolHandler;
import ai.binbun.delivery.core.ConnectorRegistry;
import ai.binbun.delivery.webhook.WebhookChannelConnector;
import ai.binbun.gateway.*;
import ai.binbun.gateway.GatewayInboundDeliveryHandler;
import ai.binbun.gateway.health.GatewayHealthService;
import ai.binbun.gateway.health.GatewayReadinessSnapshotService;
import ai.binbun.gateway.health.GatewaySubsystemStatus;
import ai.binbun.gateway.recovery.GatewayRecoveryCoordinator;
import ai.binbun.memory.JsonSessionRepository;
import ai.binbun.workflows.JsonWorkflowRunRepository;
import ai.binbun.workflows.WorkflowEngine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2FinalReadinessHttpTest {
    @Test
    void readinessEndpointShowsDegradedWithoutFailingLiveness() throws Exception {
        int port = freePort();
        try (var server = newServer(port)) {
            server.start();
            var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/gateway/readiness")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"liveness\":\"UP\""));
            assertTrue(response.body().contains("\"degraded\":true"));
            assertTrue(response.body().contains("delivery"));
        }
    }

    private AcpHttpTransportServer newServer(int port) throws IOException {
        var temp = Files.createTempDirectory("binbun-phase2-final-readiness");
        var workflowRuns = new JsonWorkflowRunRepository(temp.resolve("wf"));
        var runtime = new GatewayRuntime(new SessionRegistry(), new GatewayEventBus(), new WorkflowEngine(workflowRuns), workflowRuns, new AcpServer(), AcpProtocolDescription.defaults());
        var sessions = new GatewayAgentSessionService(runtime, new JsonSessionRepository(temp.resolve("sessions")), new GatewayModelFactory());
        var auth = new AcpAuthService(new StaticTokenAuthenticator("secret"));
        var protocolHandler = new AcpSocketProtocolHandler(auth, sessions);
        var connectors = new ConnectorRegistry();
        connectors.register(new WebhookChannelConnector("http://127.0.0.1:9999/inbox"));
        GatewayPromptRouter router = (sessionId, envelope) -> new GatewayPromptRun("run-ready", sessionId, envelope.text(), Instant.now());
        var inbound = new GatewayInboundDeliveryHandler(connectors, router);
        var readiness = new GatewayReadinessSnapshotService().snapshot(List.of(
                new GatewaySubsystemStatus("acp", "UP", "ready"),
                new GatewaySubsystemStatus("delivery", "DEGRADED", "connector outage"),
                new GatewaySubsystemStatus("plugins", "UP", "ready")
        ));
        return new AcpHttpTransportServer(runtime, sessions, protocolHandler, new GatewayHealthService(), new GatewayRecoveryCoordinator(), inbound,
                () -> Map.of("available", true), () -> readiness, () -> Map.of("available", true), () -> Map.of("available", true), port);
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
