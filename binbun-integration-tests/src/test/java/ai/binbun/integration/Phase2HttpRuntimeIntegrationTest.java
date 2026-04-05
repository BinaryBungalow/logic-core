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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2HttpRuntimeIntegrationTest {
    @Test
    void exposesGatewayHealthAndRecoveryEndpointsOverHttp() throws Exception {
        int port = freePort();
        try (var server = newServer(port, (sessionId, envelope) -> new GatewayPromptRun("run-health", sessionId, envelope.text(), Instant.now()))) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();

            var health = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/gateway/health")).GET().build(), HttpResponse.BodyHandlers.ofString());
            var recovery = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/gateway/recovery")).GET().build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(200, health.statusCode());
            assertTrue(health.body().contains("\"liveness\":\"UP\""));
            assertEquals(200, recovery.statusCode());
            assertTrue(recovery.body().contains("sessions"));
            assertTrue(recovery.body().contains("delivery"));
        }
    }

    @Test
    void acceptsInboundDeliveryOverHttp() throws Exception {
        int port = freePort();
        try (var server = newServer(port, (sessionId, envelope) -> new GatewayPromptRun("run-delivery", sessionId, envelope.text(), Instant.now()))) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();

            var response = client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/delivery/inbound?connector=webhook&sessionId=session-1"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"source\":\"integration-user\",\"text\":\"hello from http\",\"providerMessageId\":\"m-http-1\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(202, response.statusCode());
            assertTrue(response.body().contains("\"accepted\":true"));
            assertTrue(response.body().contains("run-delivery"));
            assertTrue(response.body().contains("session-1"));
        }
    }

    private AcpHttpTransportServer newServer(int port, GatewayPromptRouter router) throws IOException {
        var temp = Files.createTempDirectory("binbun-phase2-http-test");
        var workflowRuns = new JsonWorkflowRunRepository(temp.resolve("wf"));
        var runtime = new GatewayRuntime(
                new SessionRegistry(),
                new GatewayEventBus(),
                new WorkflowEngine(workflowRuns),
                workflowRuns,
                new AcpServer(),
                AcpProtocolDescription.defaults()
        );
        var sessions = new GatewayAgentSessionService(runtime, new JsonSessionRepository(temp.resolve("sessions")), new GatewayModelFactory());
        var auth = new AcpAuthService(new StaticTokenAuthenticator("secret"));
        var protocolHandler = new AcpSocketProtocolHandler(auth, sessions);
        var connectors = new ConnectorRegistry();
        connectors.register(new WebhookChannelConnector("http://127.0.0.1:9999/inbox"));
        var inbound = new GatewayInboundDeliveryHandler(connectors, router);
        return new AcpHttpTransportServer(runtime, sessions, protocolHandler, new GatewayHealthService(), new GatewayRecoveryCoordinator(), inbound, port);
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
