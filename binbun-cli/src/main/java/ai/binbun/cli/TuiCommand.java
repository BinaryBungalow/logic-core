package ai.binbun.cli;

import ai.binbun.agent.AgentRuntime;
import ai.binbun.agent.DefaultAgentSession;
import ai.binbun.agent.ToolRegistry;
import ai.binbun.agent.checkpoint.JsonSessionRepository;
import ai.binbun.agent.compactor.SummaryAwareCompactor;
import ai.binbun.agent.summary.SummaryEngine;
import ai.binbun.model.ModelClient;
import ai.binbun.model.azure.OpenAIClient;
import ai.logicbean.tui.TerminalUI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Callable;

@Command(name = "tui", description = "Start interactive terminal UI")
public final class TuiCommand implements Callable<Integer> {

    @Parameters(paramLabel = "SESSION", description = "Session ID or file path (optional)", arity = "0..1")
    private String sessionId;

    @Override
    public Integer call() throws Exception {
        String actualSessionId = sessionId == null ? UUID.randomUUID().toString() : sessionId;

        var modelClient = new OpenAIClient();
        var runtime = new AgentRuntime(modelClient);
        var checkpointStore = new JsonSessionRepository(Paths.get(System.getProperty("user.home"), ".binbun", "sessions"));
        var toolRegistry = new ToolRegistry();
        var compactor = new SummaryAwareCompactor();
        var summaryEngine = new SummaryEngine(modelClient);

        // Register all builtin tools
        toolRegistry.register(new EchoTool());
        toolRegistry.register(new ClockTool());

        var session = new DefaultAgentSession(
                actualSessionId,
                "gpt-4o",
                runtime,
                checkpointStore,
                toolRegistry,
                compactor,
                summaryEngine
        );

        var tui = TerminalUI.create();
        var layout = new AgentLayout();
        tui.setRootComponent(layout.root());

        // Register agent event bridge
        session.events().subscribe(new TuiEventBridge(tui, layout));

        // Setup keyboard shortcuts matching pi's keybindings
        layout.inputField().setSubmitHandler(value -> {
            if (!value.isBlank()) {
                layout.commitMessage("user", value);
                layout.statusBar().setText("Running...");
                session.prompt(value);
            }
            layout.inputField().clear();
        });

        tui.setGlobalKeyHandler(key -> {
            return switch (key) {
                case "ctrl+c" -> {
                    tui.shutdown();
                    session.close();
                    yield true;
                }
                case "ctrl+l" -> {
                    layout.messageView().clear();
                    yield true;
                }
                case "ctrl+r" -> {
                    session.compact(10);
                    layout.statusBar().setText("Session compacted");
                    yield true;
                }
                case "ctrl+b" -> {
                    var branch = session.branch(null);
                    layout.statusBar().setText("Branched to " + branch.id());
                    yield true;
                }
                case "escape" -> {
                    tui.focus(layout.inputField());
                    yield true;
                }
                default -> false;
            };
        });

        layout.statusBar().setText("Session: " + actualSessionId);
        tui.focus(layout.inputField());
        tui.start();

        return 0;
    }
}
