package ai.binbun.browser.playwright;

import com.microsoft.playwright.*;
import ai.binbun.browser.core.BrowserAutomationService;
import ai.binbun.browser.core.BrowserCommand;
import ai.binbun.browser.core.BrowserResult;

import java.util.concurrent.atomic.AtomicReference;

public final class PlaywrightBrowserService implements BrowserAutomationService {
    private final AtomicReference<Playwright> playwrightRef = new AtomicReference<>();
    private final AtomicReference<Browser> browserRef = new AtomicReference<>();

    public void start() {
        Playwright playwright = Playwright.create();
        playwrightRef.set(playwright);
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        browserRef.set(browser);
    }

    public void stop() {
        Browser browser = browserRef.getAndSet(null);
        if (browser != null) {
            browser.close();
        }
        Playwright playwright = playwrightRef.getAndSet(null);
        if (playwright != null) {
            playwright.close();
        }
    }

    @Override
    public BrowserResult execute(BrowserCommand command) {
        Browser browser = browserRef.get();
        if (browser == null) {
            return new BrowserResult(false, "browser not started");
        }
        try (var context = browser.newContext();
             var page = context.newPage()) {
            return switch (command.action()) {
                case "navigate" -> {
                    page.navigate(command.target());
                    String title = page.title();
                    yield new BrowserResult(true, "navigated to " + command.target() + " (title: " + title + ")");
                }
                case "click" -> {
                    page.navigate(command.value());
                    page.click(command.target());
                    yield new BrowserResult(true, "clicked " + command.target());
                }
                case "type" -> {
                    page.navigate(command.value());
                    page.fill(command.target(), command.value());
                    yield new BrowserResult(true, "typed into " + command.target());
                }
                case "readText" -> {
                    page.navigate(command.target());
                    String text = page.textContent(command.value() != null ? command.value() : "body");
                    yield new BrowserResult(true, text != null ? text : "");
                }
                case "screenshot" -> {
                    page.navigate(command.target());
                    byte[] screenshot = page.screenshot();
                    yield new BrowserResult(true, "screenshot captured (" + screenshot.length + " bytes)");
                }
                default -> new BrowserResult(false, "unknown action: " + command.action());
            };
        } catch (Exception e) {
            return new BrowserResult(false, "browser error: " + e.getMessage());
        }
    }
}
