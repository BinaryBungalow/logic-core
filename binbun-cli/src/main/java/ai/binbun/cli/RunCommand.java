package ai.binbun.cli;

import ai.binbun.agent.event.AgentEvent;
import ai.binbun.agent.AgentRuntime;
import ai.binbun.agent.DefaultAgentSession;
import ai.binbun.agent.event.MessageCommitted;
import ai.binbun.agent.event.RunFailed;
import ai.binbun.agent.event.TokenDelta;
import ai.binbun.agent.event.ToolFinished;
import ai.binbun.agent.event.ToolStarted;
import ai.binbun.agent.memory.MemoryPolicy;
import ai.binbun.agent.obs.EventSink;
import ai.binbun.agent.summary.HeuristicSummaryEngine;
import ai.binbun.agent.summary.ModelBackedSummaryEngine;
import ai.binbun.agent.summary.SummaryAwareCompactor;
import ai.binbun.agent.summary.SummaryEngine;
import ai.binbun.extensions.ExtensionExecutionContext;
import ai.binbun.extensions.ExtensionRegistry;
import ai.binbun.memory.JsonSessionRepository;
import ai.binbun.memory.JsonlEventLogRepository;
import ai.binbun.model.ModelClient;
import ai.binbun.model.ModelClients;
import ai.binbun.model.ProviderKind;
import ai.binbun.resources.DefaultResourceLoader;
import ai.binbun.skills.SkillExecutionContext;
import ai.binbun.skills.SkillRegistry;
import ai.binbun.tools.ClockTool;
import ai.binbun.tools.EchoTool;
import ai.binbun.tools.ToolRegistry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

@Command(name = "run", mixinStandardHelpOptions = true, description = "Run a single prompt")
public final class RunCommand implements Runnable {

    @Parameters(index = "0", description = "Prompt text")
    String prompt;

    @Option(names = "--provider", defaultValue = "openai", description = "Provider: ${COMPLETION-CANDIDATES}")
    String provider;

    @Option(names = "--session-id", description = "Existing or new session id")
    String sessionId;

    @Option(names = "--no-tools", description = "Disable built-in sample tools")
    boolean noTools;

    @Option(names = "--compact-after", description = "Compact session after run, keeping the last N messages")
    Integer compactAfter;

    @Option(names = "--branch-to", description = "Create a branched copy of the session after the run")
    String branchTo;

    @Option(names = "--show-resources", description = "Print resources discovered from ~/.pi-java and <project>/.pi-java")
    boolean showResources;

    @Option(names = "--use-model-summary", description = "Use the configured model to summarize older conversation during compaction")
    boolean useModelSummary;

    @Option(names = "--prompt", description = "Prompt manifest name to load", split = ",")
    List<String> promptNames = new ArrayList<>();

    @Option(names = "--skill", description = "Skill manifest name to load", split = ",")
    List<String> skillNames = new ArrayList<>();

    @Option(names = "--extension", description = "Extension manifest name to load", split = ",")
    List<String> extensionNames = new ArrayList<>();

    @Option(names = "--max-messages", description = "Auto-compact when session exceeds this many messages")
    Integer maxMessages;

    @Option(names = "--keep-last", description = "Keep this many recent messages during auto-compaction")
    Integer keepLast;

    @Option(names = "--no-event-log", description = "Disable JSONL raw/normalized event logging")
    boolean noEventLog;

    @Override
    public void run() {
        ProviderKind providerKind = ProviderKind.valueOf(provider.toUpperCase(Locale.ROOT));
        String baseUrl = resolveBaseUrl(providerKind);
        String apiKey = System.getenv().getOrDefault("BINBUN_API_KEY", "");
        String model = System.getenv().getOrDefault("BINBUN_MODEL", defaultModel(providerKind));
        String resolvedSessionId = sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;
        Path projectRoot = Path.of("").toAbsolutePath();

        var resourceLoader = new DefaultResourceLoader();
        var runtimeContext = resourceLoader.loadRuntimeContext(projectRoot, promptNames, skillNames, extensionNames);
        if (showResources) {
            for (var resource : runtimeContext.catalog().resources()) {
                System.err.println("[resource] " + resource.scope() + " " + resource.type() + " " + resource.path());
            }
            for (var promptManifest : runtimeContext.prompts()) {
                System.err.println("[prompt] " + promptManifest.name());
            }
            for (var skill : runtimeContext.skills()) {
                System.err.println("[skill] " + skill.name());
            }
            for (var extension : runtimeContext.extensions()) {
                System.err.println("[extension] " + extension.name());
            }
        }

        ModelClient modelClient = ModelClients.create(providerKind, URI.create(baseUrl), apiKey);
        var runtime = new AgentRuntime(modelClient);
        var repository = new JsonSessionRepository(Path.of(System.getProperty("user.home"), ".pi-java", "sessions"));
        EventSink eventSink = noEventLog
                ? new EventSink() {}
                : new JsonlEventLogRepository(Path.of(System.getProperty("user.home"), ".pi-java", "events"));
        var tools = new ToolRegistry();
        if (!noTools) {
            tools.register(new EchoTool());
            tools.register(new ClockTool(Clock.systemUTC()));
        }

        var skillRegistry = new SkillRegistry();
        var skillActivation = skillRegistry.activateAll(
                runtimeContext.skills(),
                runtimeContext.prompts(),
                new SkillExecutionContext(resolvedSessionId, prompt, runtimeContext.prompts().stream().map(p -> p.name()).toList()),
                tools
        );
        for (String warning : skillActivation.warnings()) {
            System.err.println("[skill-warning] " + warning);
        }

        var extensionRegistry = new ExtensionRegistry();
        var extensionActivation = extensionRegistry.activateAll(
                runtimeContext.extensions(),
                new ExtensionExecutionContext(resolvedSessionId, prompt, runtimeContext.skills().stream().map(s -> s.name()).toList()),
                tools
        );
        for (String warning : extensionActivation.warnings()) {
            System.err.println("[extension-warning] " + warning);
        }
        for (String exportedTool : extensionActivation.exportedTools()) {
            System.err.println("[extension-tool] declared " + exportedTool);
        }

        SummaryEngine summaryEngine = useModelSummary
                ? new ModelBackedSummaryEngine(modelClient, model)
                : new HeuristicSummaryEngine();
        MemoryPolicy policy = new MemoryPolicy(
                maxMessages == null ? MemoryPolicy.defaults().maxMessagesBeforeCompaction() : maxMessages,
                keepLast == null ? MemoryPolicy.defaults().keepLastMessages() : keepLast,
                true
        );

        List<String> bootstrap = new ArrayList<>();
        bootstrap.addAll(skillActivation.systemMessages());
        bootstrap.addAll(extensionActivation.systemMessages());

        try (var session = new DefaultAgentSession(resolvedSessionId, model, runtime, repository, tools,
                new SummaryAwareCompactor(), summaryEngine, policy, eventSink)) {
            session.seedSystemMessages(bootstrap);
            CountDownLatch done = new CountDownLatch(1);
            session.events().subscribe(new Flow.Subscriber<>() {
                @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
                @Override public void onNext(AgentEvent item) {
                    switch (item) {
                        case TokenDelta delta -> System.out.print(delta.text());
                        case ToolStarted started -> System.err.println("\n[tool] " + started.toolName() + " started");
                        case ToolFinished finished -> System.err.println("[tool] " + finished.toolName() + " finished: " + finished.result());
                        case MessageCommitted ignored -> {
                            System.out.println();
                            done.countDown();
                        }
                        case RunFailed failed -> {
                            System.err.println(failed.reason());
                            done.countDown();
                        }
                        default -> {
                        }
                    }
                }
                @Override public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                    done.countDown();
                }
                @Override public void onComplete() {
                }
            });
            session.prompt(prompt);
            done.await();
            if (compactAfter != null) {
                session.compact(compactAfter);
                System.err.println("[session] compacted to last " + compactAfter + " messages using " +
                        (useModelSummary ? "model-backed" : "heuristic") + " summary-aware compaction");
            }
            if (branchTo != null && !branchTo.isBlank()) {
                var branch = session.branch(branchTo);
                System.err.println("[session] branched to " + branch.sessionId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for model output", e);
        }
    }

    private String resolveBaseUrl(ProviderKind providerKind) {
        String env = System.getenv("BINBUN_BASE_URL");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return switch (providerKind) {
            case ANTHROPIC -> "https://api.anthropic.com";
            case GOOGLE -> "https://generativelanguage.googleapis.com";
            case OPENAI -> "https://api.openai.com";
        };
    }

    private String defaultModel(ProviderKind providerKind) {
        return switch (providerKind) {
            case ANTHROPIC -> "claude-sonnet-4-0";
            case GOOGLE -> "gemini-2.5-flash";
            case OPENAI -> "gpt-4.1-mini";
        };
    }
}
