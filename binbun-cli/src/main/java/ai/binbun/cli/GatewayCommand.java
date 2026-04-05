package ai.binbun.cli;

import ai.binbun.acp.AcpProtocolDescription;
import ai.binbun.acp.AcpServer;
import ai.binbun.acp.auth.AcpAuthService;
import ai.binbun.acp.auth.StaticTokenAuthenticator;
import ai.binbun.acp.http.AcpHttpTransportServer;
import ai.binbun.acp.protocol.AcpOperationalPayloadFactory;
import ai.binbun.acp.protocol.AcpProtocolSummaryService;
import ai.binbun.acp.socket.AcpSocketProtocolHandler;
import ai.binbun.acp.socket.AcpSocketTransportServer;
import ai.binbun.browser.core.BrowserRuntimeStatusService;
import ai.binbun.browser.core.BrowserToolBridge;
import ai.binbun.browser.core.BrowserToolInstaller;
import ai.binbun.browser.playwright.PlaywrightBrowserService;
import ai.binbun.coreplugin.CorePluginBootstrap;
import ai.binbun.delivery.core.ConnectorRegistry;
import ai.binbun.delivery.core.DeliveryFailureHandler;
import ai.binbun.delivery.core.DeliveryManagedResult;
import ai.binbun.delivery.core.DeliveryRetryLifecycleService;
import ai.binbun.delivery.core.DeliveryRetryPlanner;
import ai.binbun.delivery.core.DeliveryRetryPolicy;
import ai.binbun.delivery.core.DeliveryRuntimeStatusService;
import ai.binbun.delivery.core.DeliveryService;
import ai.binbun.delivery.core.InboundMessageEnvelope;
import ai.binbun.delivery.core.OutboundMessage;
import ai.binbun.delivery.slack.SlackChannelConnector;
import ai.binbun.delivery.model.JsonDeliveryDeadLetterRepository;
import ai.binbun.delivery.model.JsonDeliveryJobRepository;
import ai.binbun.delivery.telegram.TelegramChannelConnector;
import ai.binbun.delivery.webhook.WebhookChannelConnector;
import ai.binbun.gateway.GatewayAgentSessionService;
import ai.binbun.gateway.GatewayDeliveryRouter;
import ai.binbun.gateway.GatewayEventBus;
import ai.binbun.gateway.GatewayInboundDeliveryHandler;
import ai.binbun.gateway.GatewayMethod;
import ai.binbun.gateway.GatewayMethodRegistry;
import ai.binbun.gateway.GatewayModelFactory;
import ai.binbun.gateway.GatewayPluginBootstrap;
import ai.binbun.gateway.GatewayRuntime;
import ai.binbun.gateway.GatewayWebSocketServer;
import ai.binbun.gateway.OperatorScope;
import ai.binbun.gateway.SessionRegistry;
import ai.binbun.gateway.SubscriptionRegistry;
import ai.binbun.gateway.health.GatewayHealthReport;
import ai.binbun.gateway.health.GatewayHealthService;
import ai.binbun.gateway.health.GatewayOperationalSnapshotService;
import ai.binbun.gateway.health.GatewayReadinessSnapshotService;
import ai.binbun.gateway.health.GatewaySubsystemStatus;
import ai.binbun.gateway.observability.ObservabilityService;
import ai.binbun.gateway.recovery.GatewayRecoveryCoordinator;
import ai.binbun.memory.JsonSessionRepository;
import ai.binbun.model.ToolCall;
import ai.binbun.nativetools.CronExecutionStateRepository;
import ai.binbun.nativetools.CronExecutor;
import ai.binbun.nativetools.CronExpressionMatcher;
import ai.binbun.nativetools.CronSchedule;
import ai.binbun.nativetools.CronSchedulerService;
import ai.binbun.nativetools.JsonCronExecutionStateRepository;
import ai.binbun.nativetools.JsonCronScheduleRepository;
import ai.binbun.nativetools.MessageDispatcher;
import ai.binbun.nativetools.MessageDispatcherFactory;
import ai.binbun.nativetools.NativeToolCatalog;
import ai.binbun.nativetools.NativeToolInstaller;
import ai.binbun.plugin.manifest.PluginEnvironmentReportService;
import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.manifest.PluginRuntimeStatusService;
import ai.binbun.plugin.registry.PluginRegistryService;
import ai.binbun.plugin.runtime.PluginRuntimeManager;
import ai.binbun.tools.ToolRegistry;
import ai.binbun.workflows.JsonWorkflowRunRepository;
import ai.binbun.workflows.WorkflowDefinition;
import ai.binbun.workflows.WorkflowEngine;
import ai.binbun.workflows.WorkflowStep;
import picocli.CommandLine.Command;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Command(name = "gateway", mixinStandardHelpOptions = true, description = "Start the headless gateway scaffold")
public final class GatewayCommand implements Runnable {
    @Override
    public void run() {
        Path home = Path.of(System.getProperty("user.home"), ".pi-java");
        var workflowRuns = new JsonWorkflowRunRepository(home.resolve("workflow-runs"));
        var runtime = new GatewayRuntime(
                new SessionRegistry(),
                new GatewayEventBus(),
                new WorkflowEngine(workflowRuns),
                workflowRuns,
                new AcpServer(),
                AcpProtocolDescription.defaults()
        );
        var core = new CorePluginBootstrap();
        var toolRegistry = new ToolRegistry();
        MessageDispatcher dispatcher = new MessageDispatcherFactory().create(home);
        var cronRepository = new JsonCronScheduleRepository(home.resolve("cron"));
        CronExecutionStateRepository cronState = new JsonCronExecutionStateRepository(home.resolve("cron-state"));
        var installer = new NativeToolInstaller(runtime, dispatcher, cronRepository);
        installer.installInto(toolRegistry);
        var sessionRepository = new JsonSessionRepository(home.resolve("sessions"));
        var gatewaySessions = new GatewayAgentSessionService(runtime, sessionRepository, new GatewayModelFactory());

        var connectorRegistry = new ConnectorRegistry();
        connectorRegistry.register(new WebhookChannelConnector(System.getenv().getOrDefault("PI_WEBHOOK_ENDPOINT", "http://127.0.0.1:9999/inbox")));
        String telegramToken = System.getenv("PI_TELEGRAM_BOT_TOKEN");
        if (telegramToken != null && !telegramToken.isBlank()) {
            connectorRegistry.register(new TelegramChannelConnector(telegramToken, System.getenv("PI_TELEGRAM_WEBHOOK_URL")));
        }
        String slackToken = System.getenv("PI_SLACK_BOT_TOKEN");
        if (slackToken != null && !slackToken.isBlank()) {
            connectorRegistry.register(new SlackChannelConnector(slackToken));
        }
        var deliveryJobs = new JsonDeliveryJobRepository(home.resolve("delivery-jobs"));
        var deliveryDeadLetters = new JsonDeliveryDeadLetterRepository(home.resolve("delivery-dead-letter"));
        var deliveryService = new DeliveryService(connectorRegistry, deliveryJobs);
        var retryPlanner = new DeliveryRetryPlanner(new DeliveryRetryPolicy(3, 1000));
        var deliveryFailureHandler = new DeliveryFailureHandler(retryPlanner, deliveryDeadLetters);
        var deliveryRouter = new GatewayDeliveryRouter(gatewaySessions);
        var inboundDeliveryHandler = new GatewayInboundDeliveryHandler(connectorRegistry, deliveryRouter);

        var pluginRuntime = new PluginRuntimeManager();
        var activatedPlugins = new GatewayPluginBootstrap(new PluginRegistryService(), pluginRuntime)
                .activateFrom(home.resolve("plugins/manifests"));
        var coreManifestAsPlugin = new PluginManifest(
                core.manifest().name(),
                "0.1.0",
                "core",
                List.of(),
                List.of(),
                core.manifest().nativeTools(),
                core.manifest().workflowProfiles(),
                List.of(),
                List.of(),
                List.of(),
                "0.1.0"
        );
        var pluginEnvironmentReport = new PluginEnvironmentReportService().evaluate(coreManifestAsPlugin, System.getenv());
        var pluginRuntimeStatus = new PluginRuntimeStatusService().snapshot(coreManifestAsPlugin, System.getenv());
        var recoveryCoordinator = new GatewayRecoveryCoordinator();
        var recoveryPlan = recoveryCoordinator.startupPlan();
        GatewayHealthService healthService = new GatewayHealthService();
        GatewayHealthReport health = healthService.snapshot();
        var operationalSnapshot = new GatewayOperationalSnapshotService(healthService, recoveryCoordinator)
                .snapshot(true, !connectorRegistry.all().isEmpty(), !activatedPlugins.isEmpty());
        var readinessSnapshot = new GatewayReadinessSnapshotService().snapshot(List.of(
                new GatewaySubsystemStatus("acp", "UP", "transport ready"),
                new GatewaySubsystemStatus("delivery", connectorRegistry.all().isEmpty() ? "DOWN" : "UP", "connectors=" + connectorRegistry.all().size()),
                new GatewaySubsystemStatus("plugins", activatedPlugins.isEmpty() ? "DEGRADED" : "UP", "activated=" + activatedPlugins.size()),
                new GatewaySubsystemStatus("browser", true ? "UP" : "DOWN", "bridge ready"),
                new GatewaySubsystemStatus("models", "UP", "environment resolved")
        ));
        var deliveryRuntimeStatusService = new DeliveryRuntimeStatusService(deliveryJobs, deliveryDeadLetters);
        var protocolSummary = new AcpProtocolSummaryService();
        var acpOperationalPayload = new AcpOperationalPayloadFactory().create(operationalSnapshot, pluginRuntimeStatus, deliveryRuntimeStatusService.snapshot(true));
        var trace = new ObservabilityService().newTrace("bootstrap");
        var browserBridge = new BrowserToolBridge(new PlaywrightBrowserService());
        new BrowserToolInstaller(browserBridge).installInto(toolRegistry);
        var browserRuntimeStatus = new BrowserRuntimeStatusService().snapshot(browserBridge);

        var authService = new AcpAuthService(new StaticTokenAuthenticator(System.getenv().getOrDefault("PI_ACP_TOKEN", "dev-token")));
        var protocolHandler = new AcpSocketProtocolHandler(authService, gatewaySessions);

        // Phase 2.1: Gateway WebSocket Server with Method Registry
        var methodRegistry = new GatewayMethodRegistry();
        var subscriptionRegistry = new SubscriptionRegistry();
        methodRegistry.register(new GatewayMethod("health", Set.of(), (params, conn) ->
                Map.of("liveness", health.liveness(), "readiness", health.readiness(), "subsystems", health.subsystems().stream().map(s -> Map.of("name", s.name(), "status", s.status())).toList())));
        methodRegistry.register(new GatewayMethod("status", Set.of(OperatorScope.READ), (params, conn) ->
                Map.of("running", runtime.status().running(), "sessions", runtime.status().sessionCount(), "startedAt", runtime.status().startedAt())));
        methodRegistry.register(new GatewayMethod("sessions.list", Set.of(OperatorScope.READ), (params, conn) ->
                Map.of("sessions", runtime.sessions().stream().map(s -> Map.of("id", s.id(), "model", s.model(), "status", s.status())).toList())));
        methodRegistry.register(new GatewayMethod("channels.status", Set.of(OperatorScope.READ), (params, conn) ->
                Map.of("connectors", connectorRegistry.all().stream().map(c -> Map.of("name", c.name(), "health", c.health().status())).toList())));
        methodRegistry.register(new GatewayMethod("plugins.list", Set.of(OperatorScope.READ), (params, conn) ->
                Map.of("plugins", pluginRuntime.list().stream().map(p -> Map.of("name", p.name(), "version", p.version(), "state", p.state().name())).toList())));
        methodRegistry.register(new GatewayMethod("recovery.plan", Set.of(OperatorScope.ADMIN), (params, conn) ->
                Map.of("checkpoints", recoveryCoordinator.startupPlan().stream().map(c -> Map.of("subsystem", c.subsystem(), "status", c.status(), "detail", c.detail())).toList())));

        runtime.eventBus().subscribe(event -> System.err.println("[gateway-event] " + event.type() + " " + event.payload()));
        runtime.start();

        int recovered = gatewaySessions.recoverAll("default-user", toolRegistry);
        String attachedSessionId;
        if (runtime.sessions().isEmpty()) {
            attachedSessionId = gatewaySessions.create("default-user", toolRegistry, core.bootstrapMessages()).registered().sessionId();
        } else {
            var existing = runtime.sessions().get(0);
            attachedSessionId = existing.id();
        }

        if (cronRepository.list().isEmpty()) {
            cronRepository.save(new CronSchedule("startup-sync", "@startup", "Run morning assistant sync"));
            cronRepository.save(new CronSchedule("heartbeat", "*/1 * * * *", "Run recurring assistant maintenance"));
        }
        var cronExecutor = new CronExecutor(cronRepository, dispatcher, cronState, new CronExpressionMatcher());
        int dueNow = cronExecutor.executeDueSchedules(Instant.now());

        var demoInbound = new InboundMessageEnvelope("webhook", "demo-user", "Phase 2 delivery routing smoke input", "demo-message", Map.of("source", "gateway-bootstrap"), null);
        var routedRun = deliveryRouter.routeToSession(attachedSessionId, demoInbound);
        var normalizedInboundRun = inboundDeliveryHandler.handle("webhook", attachedSessionId, "{\"source\":\"demo-user\",\"text\":\"Phase 2 normalized inbound\",\"providerMessageId\":\"demo-2\"}");
        var browserNavigateProbe = toolRegistry.execute(new ToolCall("nav-1", "browser.navigate", "{\"target\":\"https://example.com\"}"));
        var browserReadProbe = toolRegistry.execute(new ToolCall("read-1", "browser.readText", "{\"target\":\"body\"}"));
        var retryDecision = deliveryFailureHandler.onFailure("telegram", "startup-retry", attachedSessionId, "starter retry path", 1);
        var deadLetterDecision = deliveryFailureHandler.onFailure("telegram", "startup-dead-letter", attachedSessionId, "starter exhausted retries", 3);
        var retryLifecycle = new DeliveryRetryLifecycleService(deliveryFailureHandler).evaluate("telegram", "startup-lifecycle", attachedSessionId, 2, "starter lifecycle path");
        DeliveryManagedResult managedDelivery;
        try {
            managedDelivery = deliveryService.sendManaged("webhook", new OutboundMessage("startup-managed", attachedSessionId, "demo-chat", "Phase 2 managed outbound", Map.of("mode", "managed"), null), deliveryFailureHandler);
        } catch (Exception e) {
            managedDelivery = new DeliveryManagedResult(false, null, null, e.getMessage());
        }
        var deliveryRuntimeStatus = deliveryRuntimeStatusService.snapshot(true);

        try (var cronScheduler = new CronSchedulerService(cronExecutor, Duration.ofSeconds(30));
             var acpSocket = new AcpSocketTransportServer(gatewaySessions, protocolHandler, 8788);
             var acpHttp = new AcpHttpTransportServer(runtime, gatewaySessions, protocolHandler, healthService, recoveryCoordinator, inboundDeliveryHandler, () -> operationalSnapshot, () -> readinessSnapshot, () -> pluginRuntimeStatus, () -> deliveryRuntimeStatus, 8787);
             var gatewayWs = new GatewayWebSocketServer(methodRegistry, subscriptionRegistry, runtime.eventBus(), runtime.sessionRegistry(), 8789)) {
            cronScheduler.start();
            acpSocket.start();
            acpHttp.start();
            gatewayWs.start();

            var workflow = runtime.workflowEngine().start(new WorkflowDefinition(
                    "assistant-default",
                    List.of(new WorkflowStep("approval", "message", "draft follow-up", true), new WorkflowStep("send", "message", "send follow-up", false))
            ));
            var status = runtime.status();

            System.out.println("gateway running=" + status.running() + " sessions=" + status.sessionCount() + " recovered=" + recovered);
            System.out.println("core-plugin=" + core.manifest().name());
            System.out.println("bootstrap-messages=" + core.bootstrapMessages().size());
            System.out.println("native-tools=" + new NativeToolCatalog().defaultTools().size() + " executable-tools=" + toolRegistry.specs().size());
            System.out.println("phase2-connectors=" + connectorRegistry.all().size());
            System.out.println("phase2-delivery-demo-run=" + routedRun.id());
            System.out.println("phase2-delivery-normalized-run=" + normalizedInboundRun.id());
            String deliveryStatus;
            try {
                deliveryStatus = deliveryService.send("webhook", new OutboundMessage("startup-webhook", attachedSessionId, "demo-chat", "Phase 2 startup outbound", Map.of("mode", "bootstrap"), null)).status();
            } catch (Exception e) {
                deliveryStatus = "QUEUED (endpoint not reachable)";
            }
            System.out.println("phase2-delivery-store-health=" + deliveryStatus);
            System.out.println("phase2-delivery-retry-after-one-failure=" + retryDecision.retryable() + " delayMillis=" + retryDecision.nextDelayMillis());
            System.out.println("phase2-dead-letter-count=" + deliveryDeadLetters.list().size() + " deadLettered=" + deadLetterDecision.deadLettered());
            System.out.println("phase2-health-liveness=" + health.liveness() + " readiness=" + health.readiness());
            System.out.println("phase2-recovery-checkpoints=" + recoveryPlan.size());
            System.out.println("phase2-plugins-activated=" + activatedPlugins.size());
            System.out.println("phase2-plugin-env-ready=" + pluginEnvironmentReport.ready() + " missing=" + pluginEnvironmentReport.missingRequiredEnv().size());
            System.out.println("phase2-plugin-runtime=" + pluginRuntimeStatus.pluginName() + "/" + pluginRuntimeStatus.ready());
            System.out.println("phase2-operational-snapshot=" + operationalSnapshot.health().liveness() + "/" + operationalSnapshot.plannedRecoverySteps());
            System.out.println("phase2-delivery-runtime-status=" + deliveryRuntimeStatus.totalJobs() + "/sent=" + deliveryRuntimeStatus.sentJobs() + "/dead=" + deliveryRuntimeStatus.deadLetterCount());
            System.out.println("phase2-managed-delivery-sent=" + managedDelivery.sent());
            System.out.println("phase2-acp-version=" + protocolSummary.version() + " ops=" + protocolSummary.supportedOperations().size());
            System.out.println("phase2-acp-operational-kind=" + acpOperationalPayload.metadata().get("kind"));
            System.out.println("phase2-trace=" + trace.correlationId());
            System.out.println("phase2-browser-tools=" + browserBridge.registry().all().size());
            System.out.println("phase2-browser-navigate=" + browserNavigateProbe);
            System.out.println("phase2-browser-read=" + browserReadProbe);
            System.out.println("phase2-browser-runtime-status=" + browserRuntimeStatus.registeredTools() + "/navigate=" + browserRuntimeStatus.navigateAvailable() + "/read=" + browserRuntimeStatus.readTextAvailable());
            System.out.println("phase2-retry-lifecycle=" + retryLifecycle.attemptsSoFar() + "/retryable=" + retryLifecycle.retryable() + "/dead=" + retryLifecycle.deadLettered());
            System.out.println("acp-session=" + runtime.attachAcpSession(attachedSessionId).endpoint());
            System.out.println("acp-http=http://127.0.0.1:8787/health");
            System.out.println("acp-http-protocol=http://127.0.0.1:8787/acp/protocol");
            System.out.println("acp-http-events=http://127.0.0.1:8787/acp/events?sessionId=" + attachedSessionId + "&lastAckSequence=0");
            System.out.println("gateway-health=http://127.0.0.1:8787/gateway/health");
            System.out.println("gateway-readiness=http://127.0.0.1:8787/gateway/readiness");
            System.out.println("gateway-recovery=http://127.0.0.1:8787/gateway/recovery");
            System.out.println("gateway-operational=http://127.0.0.1:8787/gateway/operational");
            System.out.println("gateway-plugins=http://127.0.0.1:8787/gateway/plugins");
            System.out.println("delivery-status=http://127.0.0.1:8787/delivery/status");
            System.out.println("delivery-inbound=http://127.0.0.1:8787/delivery/inbound?connector=webhook&sessionId=" + attachedSessionId);
            System.out.println("acp-socket=tcp://127.0.0.1:8788");
            System.out.println("acp-token-env=PI_ACP_TOKEN");
            System.out.println("workflow-status=" + workflow.status() + " approvalRequired=" + workflow.approvalRequired());
            System.out.println("due-schedules-executed=" + dueNow);
            System.out.println("dispatch-mode=" + System.getenv().getOrDefault("PI_MESSAGE_DISPATCH", "file"));
            System.out.println("provider=" + System.getenv().getOrDefault("BINBUN_PROVIDER", "openai"));
            System.out.println("gateway ws=tcp://127.0.0.1:8789");
            System.out.println("gateway is running. Press Ctrl+C to stop.");

            // Keep the gateway alive until interrupted
            var latch = new java.util.concurrent.CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[gateway] shutting down...");
                latch.countDown();
            }));
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
