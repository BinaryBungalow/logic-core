package ai.binbun.cli;

import ai.binbun.agent.event.AgentEvent;
import ai.binbun.agent.event.MessageCommitted;
import ai.binbun.agent.event.RunFailed;
import ai.binbun.agent.event.TokenDelta;
import ai.binbun.agent.event.ToolFinished;
import ai.binbun.agent.event.ToolRequested;
import ai.binbun.agent.event.ToolStarted;
import ai.logicbean.tui.TerminalUI;

import java.util.concurrent.Flow;

final class TuiEventBridge implements Flow.Subscriber<AgentEvent> {

    private final TerminalUI tui;
    private final AgentLayout layout;

    TuiEventBridge(TerminalUI tui, AgentLayout layout) {
        this.tui = tui;
        this.layout = layout;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(AgentEvent event) {
        // Always dispatch on virtual thread, never block TUI render thread
        Thread.ofVirtual().start(() -> {
            switch (event) {
                case TokenDelta e -> {
                    layout.appendText(e.delta());
                    tui.requestRender();
                }
                case MessageCommitted e -> {
                    layout.commitMessage(e.role(), e.content());
                    layout.statusBar().setText("Ready");
                    tui.requestRender();
                }
                case ToolRequested e -> {
                    layout.statusBar().setText("Waiting for " + e.toolName());
                    tui.requestRender();
                }
                case ToolStarted e -> {
                    layout.statusBar().setText("Running " + e.toolName());
                    tui.requestRender();
                }
                case ToolFinished e -> {
                    layout.statusBar().setText("Completed " + e.toolName());
                    tui.requestRender();
                }
                case RunFailed e -> {
                    layout.statusBar().setText("Error: " + e.message());
                    tui.requestRender();
                }
                default -> {}
            }
        });
    }

    @Override
    public void onError(Throwable throwable) {
        tui.showError(throwable.getMessage());
    }

    @Override
    public void onComplete() {
    }
}
