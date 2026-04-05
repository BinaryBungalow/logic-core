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

import static org.junit.jupiter.api.Assertions.assertEquals;

class Phase2FinalWebhookVerificationTest {
    @Test
    void inboundWebhookRequiresValidSignatureWhenExpected() throws Exception {
        int port = freePort();
        try (var server = newServer(port)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            var invalid = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/delivery/inbound?connector=webhook&sessionId=session-1&expectedSignature=sig-ok"))
                    .header("X-Webhook-Signature", "sig-bad")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"source\":\"u\",\"text\":\"hello\",\"providerMessageId\":\"m1\"}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            var valid = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/delivery/inbound?connector=webhook&sessionId=session-1&expectedSignature=sig-ok"))
                    .header("X-Webhook-Signature", "sig-ok")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"source\":\"u\",\"text\":\"hello\",\"providerMessageId\":\"m1\"}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(401, invalid.statusCode());
            assertEquals(202, valid.statusCode());
        }
    }

    private AcpHttpTransportServer newServer(int port) throws IOException {
        var temp = Files.createTempDirectory("binbun-phase2-final-webhook");
        var workflowRuns = new JsonWorkflowRunRepository(temp.resolve("wf"));
        var runtime = new GatewayRuntime(new SessionRegistry(), new GatewayEventBus(), new WorkflowEngine(workflowRuns), workflowRuns, new AcpServer(), AcpProtocolDescription.defaults());
        var sessions = new GatewayAgentSessionService(runtime, new JsonSessionRepository(temp.resolve("sessions")), new GatewayModelFactory());
        var auth = new AcpAuthService(new StaticTokenAuthenticator("secret"));
        var protocolHandler = new AcpSocketProtocolHandler(auth, sessions);
        var connectors = new ConnectorRegistry();
        connectors.register(new WebhookChannelConnector("http://127.0.0.1:9999/inbox"));
        GatewayPromptRouter router = (sessionId, envelope) -> new GatewayPromptRun("run-webhook", sessionId, envelope.text(), Instant.now());
        var inbound = new GatewayInboundDeliveryHandler(connectors, router);
        sessions.create("owner", new ToolRegistry(), java.util.List.of());
        return new AcpHttpTransportServer(runtime, sessions, protocolHandler, new GatewayHealthService(), new GatewayRecoveryCoordinator(), inbound, port);
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
