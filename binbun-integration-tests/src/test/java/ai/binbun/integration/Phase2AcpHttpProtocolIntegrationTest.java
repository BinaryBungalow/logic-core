package ai.binbun.integration;

import ai.binbun.acp.AcpProtocolDescription;
import ai.binbun.acp.AcpServer;
import ai.binbun.acp.auth.AcpAuthService;
import ai.binbun.acp.auth.StaticTokenAuthenticator;
import ai.binbun.acp.http.AcpHttpProtocolAdapter;
import ai.binbun.acp.http.AcpHttpTransportServer;
import ai.binbun.acp.protocol.AcpEnvelopes;
import ai.binbun.acp.protocol.AcpOperation;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2AcpHttpProtocolIntegrationTest {
    @Test
    void supportsHelloAndAuthOverHttpProtocolRoute() throws Exception {
        int port = freePort();
        try (var server = newServer(port)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            AcpHttpProtocolAdapter adapter = new AcpHttpProtocolAdapter();

            var hello = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/acp/protocol"))
                    .header("X-ACP-Client", "client-1")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(adapter.encode(AcpEnvelopes.request(AcpOperation.HELLO, null, 1L, "hello-1", Map.of("agent", "integration-test")))))
                    .build(), HttpResponse.BodyHandlers.ofString());
            var helloEnvelope = adapter.decode(hello.body());

            var auth = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/acp/protocol"))
                    .header("X-ACP-Client", "client-1")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(adapter.encode(AcpEnvelopes.request(AcpOperation.AUTH, null, 2L, "auth-1", Map.of("token", "secret")))))
                    .build(), HttpResponse.BodyHandlers.ofString());
            var authEnvelope = adapter.decode(auth.body());

            assertEquals(200, hello.statusCode());
            assertEquals(AcpOperation.READY, helloEnvelope.op());
            assertEquals(200, auth.statusCode());
            assertEquals(AcpOperation.AUTH, authEnvelope.op());
            assertEquals("subject", String.valueOf(authEnvelope.payload().get("subject")));
        }
    }

    private AcpHttpTransportServer newServer(int port) throws IOException {
        var temp = Files.createTempDirectory("binbun-phase2-acp-http");
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
        GatewayPromptRouter router = (sessionId, envelope) -> new GatewayPromptRun("run-acp", sessionId, envelope.text(), Instant.now());
        var inbound = new GatewayInboundDeliveryHandler(connectors, router);
        return new AcpHttpTransportServer(runtime, sessions, protocolHandler, new GatewayHealthService(), new GatewayRecoveryCoordinator(), inbound, port);
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
