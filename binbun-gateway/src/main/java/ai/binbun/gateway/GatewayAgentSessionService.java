package ai.binbun.gateway;

import ai.binbun.acp.AcpSessionService;
import ai.binbun.agent.AgentRuntime;
import ai.binbun.agent.DefaultAgentSession;
import ai.binbun.agent.RunHandle;
import ai.binbun.agent.SessionSnapshot;
import ai.binbun.agent.summary.HeuristicSummaryEngine;
import ai.binbun.agent.summary.SummaryAwareCompactor;
import ai.binbun.memory.JsonSessionRepository;
import ai.binbun.model.ModelClient;
import ai.binbun.tools.ToolRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GatewayAgentSessionService implements AcpSessionService {
    private final GatewayRuntime runtime;
    private final JsonSessionRepository sessionRepository;
    private final GatewayModelFactory modelFactory;
    private final Map<String, GatewayManagedSession> liveSessions = new ConcurrentHashMap<>();

    public GatewayAgentSessionService(GatewayRuntime runtime, JsonSessionRepository sessionRepository, GatewayModelFactory modelFactory) {
        this.runtime = runtime;
        this.sessionRepository = sessionRepository;
        this.modelFactory = modelFactory;
    }

    public int recoverAll(String owner, ToolRegistry tools) {
        GatewayModelConfig config = modelFactory.resolveFromEnvironment();
        int recovered = 0;
        for (SessionSnapshot snapshot : sessionRepository.list()) {
            GatewayManagedSession session = instantiate(snapshot.sessionId(), owner, config, tools, List.of(), "RECOVERED");
            liveSessions.put(snapshot.sessionId(), session);
            recovered++;
        }
        return recovered;
    }

    public GatewayManagedSession create(String owner, ToolRegistry tools, List<String> bootstrapMessages) {
        return create(owner, modelFactory.resolveFromEnvironment(), tools, bootstrapMessages);
    }

    public GatewayManagedSession create(String owner, GatewayModelConfig config, ToolRegistry tools, List<String> bootstrapMessages) {
        String sessionId = UUID.randomUUID().toString();
        GatewayManagedSession session = instantiate(sessionId, owner, config, tools, bootstrapMessages, "READY");
        liveSessions.put(sessionId, session);
        return session;
    }

    public Optional<ManagedSession> find(String sessionId) {
        return Optional.ofNullable(liveSessions.get(sessionId)).map(s -> new ManagedSession(s.registered().sessionId(), s.session().events()));
    }

    public GatewayPromptRun prompt(String sessionId, String input) {
        GatewayManagedSession managed = require(sessionId);
        RunHandle handle = managed.session().prompt(input);
        return new GatewayPromptRun(handle.id(), sessionId, input, java.time.Instant.now());
    }

    @Override
    public AcpRunResult promptAcp(String sessionId, String input) {
        GatewayPromptRun run = prompt(sessionId, input);
        return new AcpRunResult(run.id());
    }

    public void close(String sessionId) {
        GatewayManagedSession managed = liveSessions.remove(sessionId);
        if (managed == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        managed.session().close();
        runtime.eventBus().publish(new GatewayEvent("session.closed", sessionId));
    }

    private GatewayManagedSession require(String sessionId) {
        GatewayManagedSession managed = liveSessions.get(sessionId);
        if (managed == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        return managed;
    }

    private GatewayManagedSession instantiate(String sessionId, String owner, GatewayModelConfig config,
                                              ToolRegistry tools, List<String> bootstrapMessages, String state) {
        ModelClient modelClient = modelFactory.create(config);
        var session = new DefaultAgentSession(
                sessionId,
                config.model(),
                new AgentRuntime(modelClient),
                sessionRepository,
                tools,
                new SummaryAwareCompactor(),
                new HeuristicSummaryEngine()
        );
        session.seedSystemMessages(bootstrapMessages);
        var registered = new RegisteredSession(sessionId, owner, config.model(), state);
        runtime.registerSession(registered);
        runtime.registerAcpSession(sessionId, "tcp://127.0.0.1:8788/session/" + sessionId);
        runtime.eventBus().publish(new GatewayEvent("session.ready", sessionId));
        return new GatewayManagedSession(registered, session);
    }
}
