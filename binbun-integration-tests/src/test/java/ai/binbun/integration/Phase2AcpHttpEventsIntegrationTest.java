package ai.binbun.integration;

import ai.binbun.acp.protocol.AcpEnvelope;
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
import ai.binbun.tools.ToolRegistry;
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

class Phase2AcpHttpEventsIntegrationTest {
    @Test
    void supportsAttachAndEventHeartbeatOverHttp() throws Exception {
        int port = freePort();
        try (var fixture = newFixture(port)) {
            fixture.server.start();
            HttpClient client = HttpClient.newHttpClient();
            AcpHttpProtocolAdapter adapter = new AcpHttpProtocolAdapter();

            var hello = postProtocol(client, port, "client-events", adapter,
                    AcpEnvelopes.request(AcpOperation.HELLO, null, 1L, "hello-1", Map.of("agent", "integration-test")));
            var auth = postProtocol(client, port, "client-events", adapter,
                    AcpEnvelopes.request(AcpOperation.AUTH, null, 2L, "auth-1", Map.of("token", "secret")));
            var attach = postProtocol(client, port, "client-events", adapter,
                    AcpEnvelopes.request(AcpOperation.ATTACH, fixture.managed.registered().sessionId(), 3L, "attach-1", Map.of("lastAckSequence", 0)));

            var events = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/acp/events?sessionId=" + fixture.managed.registered().sessionId() + "&lastAckSequence=0"))
                    .header("X-ACP-Client", "client-events")
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(AcpOperation.READY, adapter.decode(hello.body()).op());
            assertEquals(AcpOperation.AUTH, adapter.decode(auth.body()).op());
            var attachEnvelope = adapter.decode(attach.body());
            assertEquals(AcpOperation.ATTACH, attachEnvelope.op());
            assertEquals(true, attachEnvelope.payload().get("attached"));
            assertEquals(200, events.statusCode());
            assertTrue(events.body().contains("event: heartbeat") || events.body().contains("event: acp"));
            assertTrue(events.body().contains(fixture.managed.registered().sessionId()));
        }
    }

    private HttpResponse<String> postProtocol(HttpClient client, int port, String clientId, AcpHttpProtocolAdapter adapter, AcpEnvelope envelope) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/acp/protocol"))
                .header("X-ACP-Client", clientId)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(adapter.encode(envelope)))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private Fixture newFixture(int port) throws IOException {
        var temp = Files.createTempDirectory("binbun-phase2-acp-events");
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
        GatewayManagedSession managed = sessions.create("owner", new ToolRegistry(), List.of("system"));
        var auth = new AcpAuthService(new StaticTokenAuthenticator("secret"));
        var protocolHandler = new AcpSocketProtocolHandler(auth, sessions);
        var connectors = new ConnectorRegistry();
        connectors.register(new WebhookChannelConnector("http://127.0.0.1:9999/inbox"));
        GatewayPromptRouter router = (sessionId, envelope) -> new GatewayPromptRun("run-events", sessionId, envelope.text(), Instant.now());
        var inbound = new GatewayInboundDeliveryHandler(connectors, router);
        var server = new AcpHttpTransportServer(runtime, sessions, protocolHandler, new GatewayHealthService(), new GatewayRecoveryCoordinator(), inbound, port);
        return new Fixture(server, managed);
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private record Fixture(AcpHttpTransportServer server, GatewayManagedSession managed) implements AutoCloseable {
        @Override
        public void close() {
            server.close();
        }
    }
}
