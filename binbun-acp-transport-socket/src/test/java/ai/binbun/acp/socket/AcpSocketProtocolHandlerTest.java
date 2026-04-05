package ai.binbun.acp.socket;

import ai.binbun.acp.auth.AcpPrincipal;
import ai.binbun.acp.AcpProtocolDescription;
import ai.binbun.acp.AcpServer;
import ai.binbun.acp.auth.AcpAuthService;
import ai.binbun.acp.auth.StaticTokenAuthenticator;
import ai.binbun.acp.protocol.AcpEnvelopes;
import ai.binbun.acp.protocol.AcpOperation;
import ai.binbun.gateway.GatewayAgentSessionService;
import ai.binbun.gateway.GatewayEventBus;
import ai.binbun.gateway.GatewayModelConfig;
import ai.binbun.gateway.GatewayModelFactory;
import ai.binbun.gateway.GatewayRuntime;
import ai.binbun.gateway.SessionRegistry;
import ai.binbun.memory.JsonSessionRepository;
import ai.binbun.model.ProviderKind;
import ai.binbun.tools.ToolRegistry;
import ai.binbun.workflows.JsonWorkflowRunRepository;
import ai.binbun.workflows.WorkflowEngine;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcpSocketProtocolHandlerTest {
    @Test
    void rejectsAttachBeforeAuth() throws Exception {
        var temp = Files.createTempDirectory("binbun-acp-test");
        var runtime = new GatewayRuntime(new SessionRegistry(), new GatewayEventBus(),
                new WorkflowEngine(new JsonWorkflowRunRepository(temp.resolve("wf"))),
                new JsonWorkflowRunRepository(temp.resolve("wf")), new AcpServer(), AcpProtocolDescription.defaults());
        var sessions = new GatewayAgentSessionService(runtime, new JsonSessionRepository(temp.resolve("sessions")), new GatewayModelFactory());
        var handler = new AcpSocketProtocolHandler(new AcpAuthService(new StaticTokenAuthenticator("secret")), sessions);
        var state = new AcpSocketConnectionState();

        var response = handler.handle(AcpEnvelopes.request(AcpOperation.ATTACH, "missing", 1L, "c1", Map.of()), state);
        assertEquals("auth_required", response.error().code());
    }

    @Test
    void supportsHelloAuthAttachFlowAndResumeHint() throws Exception {
        var temp = Files.createTempDirectory("binbun-acp-test");
        var workflowRuns = new JsonWorkflowRunRepository(temp.resolve("wf"));
        var runtime = new GatewayRuntime(new SessionRegistry(), new GatewayEventBus(),
                new WorkflowEngine(workflowRuns), workflowRuns, new AcpServer(), AcpProtocolDescription.defaults());
        var sessions = new GatewayAgentSessionService(runtime, new JsonSessionRepository(temp.resolve("sessions")), new GatewayModelFactory());
        var managed = sessions.create("owner",
                new GatewayModelConfig(ProviderKind.OPENAI, URI.create("http://127.0.0.1:8080"), "test-key", "gpt-4.1-mini"),
                new ToolRegistry(), List.of("system"));
        var handler = new AcpSocketProtocolHandler(new AcpAuthService(new StaticTokenAuthenticator("secret")), sessions);
        var state = new AcpSocketConnectionState();

        var hello = handler.handle(AcpEnvelopes.request(AcpOperation.HELLO, null, 1L, "c1", Map.of("agent", "test")), state);
        var auth = handler.handle(AcpEnvelopes.request(AcpOperation.AUTH, null, 2L, "c2", Map.of("token", "secret")), state);
        var attach = handler.handle(AcpEnvelopes.request(AcpOperation.ATTACH, managed.registered().sessionId(), 3L, "c3", Map.of("lastAckSequence", 7L)), state);

        assertEquals(AcpOperation.READY, hello.op());
        assertEquals(AcpOperation.AUTH, auth.op());
        assertEquals(AcpOperation.ATTACH, attach.op());
        assertEquals(true, attach.payload().get("attached"));
        assertEquals(7L, attach.payload().get("resumeFrom"));
    }

    @Test
    void ackUpdatesConnectionState() throws Exception {
        var temp = Files.createTempDirectory("binbun-acp-test");
        var workflowRuns = new JsonWorkflowRunRepository(temp.resolve("wf"));
        var runtime = new GatewayRuntime(new SessionRegistry(), new GatewayEventBus(),
                new WorkflowEngine(workflowRuns), workflowRuns, new AcpServer(), AcpProtocolDescription.defaults());
        var sessions = new GatewayAgentSessionService(runtime, new JsonSessionRepository(temp.resolve("sessions")), new GatewayModelFactory());
        var handler = new AcpSocketProtocolHandler(new AcpAuthService(new StaticTokenAuthenticator("secret")), sessions);
        var state = new AcpSocketConnectionState();
        state.markHelloComplete();
        state.authenticate(new AcpPrincipal("subject", "client"));
        state.attachSession("s1");

        var ack = handler.handle(AcpEnvelopes.request(AcpOperation.ACK, "s1", 5L, "c5", Map.of("sequence", 42L)), state);

        assertEquals(42L, state.lastAcknowledgedSequence());
        assertEquals(42L, ack.payload().get("acknowledged"));
    }

    @Test
    void concurrentClientsCanAttachSameSessionIndependently() throws Exception {
        var temp = Files.createTempDirectory("binbun-acp-test");
        var workflowRuns = new JsonWorkflowRunRepository(temp.resolve("wf"));
        var runtime = new GatewayRuntime(new SessionRegistry(), new GatewayEventBus(),
                new WorkflowEngine(workflowRuns), workflowRuns, new AcpServer(), AcpProtocolDescription.defaults());
        var sessions = new GatewayAgentSessionService(runtime, new JsonSessionRepository(temp.resolve("sessions")), new GatewayModelFactory());
        var managed = sessions.create("owner",
                new GatewayModelConfig(ProviderKind.OPENAI, URI.create("http://127.0.0.1:8080"), "test-key", "gpt-4.1-mini"),
                new ToolRegistry(), List.of("system"));
        var handler = new AcpSocketProtocolHandler(new AcpAuthService(new StaticTokenAuthenticator("secret")), sessions);
        var stateA = new AcpSocketConnectionState();
        var stateB = new AcpSocketConnectionState();

        handler.handle(AcpEnvelopes.request(AcpOperation.HELLO, null, 1L, "a1", Map.of()), stateA);
        handler.handle(AcpEnvelopes.request(AcpOperation.AUTH, null, 2L, "a2", Map.of("token", "secret")), stateA);
        var attachA = handler.handle(AcpEnvelopes.request(AcpOperation.ATTACH, managed.registered().sessionId(), 3L, "a3", Map.of("lastAckSequence", 4L)), stateA);

        handler.handle(AcpEnvelopes.request(AcpOperation.HELLO, null, 1L, "b1", Map.of()), stateB);
        handler.handle(AcpEnvelopes.request(AcpOperation.AUTH, null, 2L, "b2", Map.of("token", "secret")), stateB);
        var attachB = handler.handle(AcpEnvelopes.request(AcpOperation.ATTACH, managed.registered().sessionId(), 3L, "b3", Map.of("lastAckSequence", 9L)), stateB);

        assertEquals(true, attachA.payload().get("attached"));
        assertEquals(4L, attachA.payload().get("resumeFrom"));
        assertEquals(true, attachB.payload().get("attached"));
        assertEquals(9L, attachB.payload().get("resumeFrom"));
    }
}
