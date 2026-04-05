package ai.binbun.browser.core;

public record BrowserRuntimeStatus(
        int registeredTools,
        boolean navigateAvailable,
        boolean readTextAvailable
) {
}
