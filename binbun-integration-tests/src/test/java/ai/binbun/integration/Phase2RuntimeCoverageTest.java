package ai.binbun.integration;

import ai.binbun.browser.core.BrowserToolBridge;
import ai.binbun.browser.core.BrowserToolInstaller;
import ai.binbun.browser.playwright.PlaywrightBrowserService;
import ai.binbun.delivery.core.ConnectorRegistry;
import ai.binbun.delivery.webhook.WebhookChannelConnector;
import ai.binbun.gateway.GatewayInboundDeliveryHandler;
import ai.binbun.gateway.GatewayPluginBootstrap;
import ai.binbun.gateway.GatewayPromptRun;
import ai.binbun.gateway.GatewayPromptRouter;
import ai.binbun.gateway.health.GatewayHealthService;
import ai.binbun.gateway.recovery.GatewayRecoveryCoordinator;
import ai.binbun.plugin.registry.PluginRegistryService;
import ai.binbun.plugin.runtime.PluginRuntimeManager;
import ai.binbun.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2RuntimeCoverageTest {
    @Test
    void browserToolsRegisterIntoToolRegistry() {
        var tools = new ToolRegistry();
        new BrowserToolInstaller(new BrowserToolBridge(new PlaywrightBrowserService())).installInto(tools);
        assertEquals(4, tools.specs().stream().filter(spec -> spec.name().startsWith("browser.")).count());
    }

    @Test
    void gatewayStatusServicesExposeStarterSnapshots() {
        assertEquals("UP", new GatewayHealthService().snapshot().liveness());
        assertFalse(new GatewayRecoveryCoordinator().startupPlan().isEmpty());
    }

    @Test
    void discoveredPluginManifestCanBeActivatedFromDisk() {
        var activated = new GatewayPluginBootstrap(new PluginRegistryService(), new PluginRuntimeManager())
                .activateFrom(Path.of("src/test/resources/plugins/manifests"));
        assertTrue(activated.contains("discovered-plugin"));
    }

    @Test
    void webhookConnectorNormalizesInboundPayload() {
        var connector = new WebhookChannelConnector("http://127.0.0.1:9999/inbox");
        var envelope = connector.normalizeInbound("{\"source\":\"integration-user\",\"text\":\"hello\",\"providerMessageId\":\"m1\"}");
        assertEquals("integration-user", envelope.source());
        assertEquals("hello", envelope.text());
        assertEquals("m1", envelope.providerMessageId());
    }

    @Test
    void deliveryHandlerUsesRegisteredConnector() {
        var connectors = new ConnectorRegistry();
        connectors.register(new WebhookChannelConnector("http://127.0.0.1:9999/inbox"));
        GatewayPromptRouter router = (sessionId, envelope) -> new GatewayPromptRun("run-1", sessionId, envelope.text(), java.time.Instant.now());
        var handler = new GatewayInboundDeliveryHandler(connectors, router);
        var run = handler.handle("webhook", "session-1", "{\"source\":\"integration-user\",\"text\":\"hello\",\"providerMessageId\":\"m1\"}");
        assertEquals("run-1", run.id());
    }
}
