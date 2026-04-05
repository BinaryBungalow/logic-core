package ai.binbun.integration;

import ai.binbun.acp.AcpProtocolDescription;
import ai.binbun.acp.AcpServer;
import ai.binbun.acp.auth.AcpAuthService;
import ai.binbun.acp.auth.StaticTokenAuthenticator;
import ai.binbun.acp.http.AcpHttpTransportServer;
import ai.binbun.acp.socket.AcpSocketProtocolHandler;
import ai.binbun.delivery.core.ConnectorRegistry;
import ai.binbun.delivery.core.DeliveryRuntimeStatusService;
import ai.binbun.delivery.webhook.WebhookChannelConnector;
import ai.binbun.delivery.model.JsonDeliveryDeadLetterRepository;
import ai.binbun.delivery.model.JsonDeliveryJobRepository;
import ai.binbun.gateway.*;
import ai.binbun.gateway.GatewayInboundDeliveryHandler;
import ai.binbun.gateway.health.GatewayHealthService;
import ai.binbun.gateway.health.GatewayOperationalSnapshotService;
import ai.binbun.gateway.recovery.GatewayRecoveryCoordinator;
import ai.binbun.memory.JsonSessionRepository;
import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.manifest.PluginRuntimeStatusService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2BroadRuntimeEndpointsTest {
    @Test
    void exposesPluginAndDeliveryStatusOverHttp() throws Exception {
        int port = freePort();
        try (var server = newServer(port)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            var plugins = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/gateway/plugins")).GET().build(), HttpResponse.BodyHandlers.ofString());
            var delivery = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/delivery/status")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, plugins.statusCode());
            assertTrue(plugins.body().contains("pluginName"));
            assertTrue(plugins.body().contains("missingRequiredEnvCount"));
            assertEquals(200, delivery.statusCode());
            assertTrue(delivery.body().contains("retryEnabled"));
            assertTrue(delivery.body().contains("deadLetterCount"));
        }
    }

    private AcpHttpTransportServer newServer(int port) throws IOException {
        var temp = Files.createTempDirectory("binbun-phase2-broad-endpoints");
        var workflowRuns = new JsonWorkflowRunRepository(temp.resolve("wf"));
        var runtime = new GatewayRuntime(new SessionRegistry(), new GatewayEventBus(), new WorkflowEngine(workflowRuns), workflowRuns, new AcpServer(), AcpProtocolDescription.defaults());
        var sessions = new GatewayAgentSessionService(runtime, new JsonSessionRepository(temp.resolve("sessions")), new GatewayModelFactory());
        var auth = new AcpAuthService(new StaticTokenAuthenticator("secret"));
        var protocolHandler = new AcpSocketProtocolHandler(auth, sessions);
        var connectors = new ConnectorRegistry();
        connectors.register(new WebhookChannelConnector("http://127.0.0.1:9999/inbox"));
        GatewayPromptRouter router = (sessionId, envelope) -> new GatewayPromptRun("run-broad", sessionId, envelope.text(), Instant.now());
        var inbound = new GatewayInboundDeliveryHandler(connectors, router);
        var operational = new GatewayOperationalSnapshotService(new GatewayHealthService(), new GatewayRecoveryCoordinator()).snapshot(true, true, false);
        var pluginStatus = new PluginRuntimeStatusService().snapshot(new PluginManifest("plugin-c", "0.1.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of("API_KEY"), List.of(), "phase-2"), System.getenv());
        var deliveryStatus = new DeliveryRuntimeStatusService(new JsonDeliveryJobRepository(temp.resolve("jobs")), new JsonDeliveryDeadLetterRepository(temp.resolve("dead"))).snapshot(true);
        return new AcpHttpTransportServer(runtime, sessions, protocolHandler, new GatewayHealthService(), new GatewayRecoveryCoordinator(), inbound, () -> operational, () -> pluginStatus, () -> deliveryStatus, port);
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
