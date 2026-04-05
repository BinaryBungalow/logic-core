package ai.binbun.gateway;

import ai.binbun.acp.AcpGateway;
import ai.binbun.acp.AcpProtocolDescription;
import ai.binbun.acp.AcpServer;
import ai.binbun.workflows.WorkflowEngine;
import ai.binbun.workflows.WorkflowRunRepository;

import java.util.List;
import java.util.function.Consumer;

public final class GatewayRuntime implements AcpGateway {
    private final SessionRegistry sessionRegistry;
    private final GatewayEventBus eventBus;
    private final GatewayServer server;
    private final WorkflowEngine workflowEngine;
    private final WorkflowRunRepository workflowRuns;
    private final AcpServer acpServer;
    private final AcpProtocolDescription acpProtocol;

    public GatewayRuntime(SessionRegistry sessionRegistry, GatewayEventBus eventBus,
                          WorkflowEngine workflowEngine, WorkflowRunRepository workflowRuns,
                          AcpServer acpServer, AcpProtocolDescription acpProtocol) {
        this.sessionRegistry = sessionRegistry;
        this.eventBus = eventBus;
        this.server = new GatewayServer(sessionRegistry, eventBus);
        this.workflowEngine = workflowEngine;
        this.workflowRuns = workflowRuns;
        this.acpServer = acpServer;
        this.acpProtocol = acpProtocol;
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }

    public GatewayStatus status() {
        return server.status();
    }

    @Override
    public String name() {
        return "gateway";
    }

    @Override
    public List<AcpGateway.AcpSessionInfo> sessions() {
        return sessionRegistry.list().stream()
                .map(s -> new AcpGateway.AcpSessionInfo(s.sessionId(), s.model(), s.state()))
                .toList();
    }

    @Override
    public AcpGateway.AcpSessionHandle attachAcpSession(String sessionId) {
        var handle = acpServer.find(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ACP session: " + sessionId));
        return new AcpGateway.AcpSessionHandle(handle.sessionId(), handle.endpoint());
    }

    @Override
    public void subscribeEvents(Consumer<AcpGateway.AcpEvent> consumer) {
        eventBus.subscribe(event -> consumer.accept(new AcpGateway.AcpEvent(event.type(), event.payload())));
    }

    public void registerSession(RegisteredSession session) {
        sessionRegistry.register(session);
        eventBus.publish(new GatewayEvent("session.registered", session.sessionId()));
    }

    public void registerAcpSession(String sessionId, String endpoint) {
        acpServer.register(new AcpGateway.AcpSessionHandle(sessionId, endpoint));
        eventBus.publish(new GatewayEvent("acp.session.registered", sessionId));
    }

    public WorkflowEngine workflowEngine() {
        return workflowEngine;
    }

    public WorkflowRunRepository workflowRuns() {
        return workflowRuns;
    }

    public GatewayEventBus eventBus() {
        return eventBus;
    }

    public SessionRegistry sessionRegistry() {
        return sessionRegistry;
    }
}
