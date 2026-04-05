package ai.binbun.integration;

import ai.binbun.acp.protocol.AcpOperation;
import ai.binbun.acp.protocol.AcpProtocolSummaryService;
import ai.binbun.browser.core.BrowserToolBridge;
import ai.binbun.browser.core.BrowserToolInstaller;
import ai.binbun.browser.playwright.PlaywrightBrowserService;
import ai.binbun.delivery.core.DeliveryRetryPlanner;
import ai.binbun.delivery.core.DeliveryRetryPolicy;
import ai.binbun.delivery.model.DeliveryDeadLetterRecord;
import ai.binbun.delivery.model.JsonDeliveryDeadLetterRepository;
import ai.binbun.gateway.health.GatewayHealthService;
import ai.binbun.gateway.health.GatewayOperationalSnapshotService;
import ai.binbun.gateway.recovery.GatewayRecoveryCoordinator;
import ai.binbun.plugin.manifest.PluginEnvironmentValidator;
import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2BalancedBreadthTest {
    @Test
    void exposesAcpProtocolSummaryAcrossOperations() {
        var summary = new AcpProtocolSummaryService();
        assertEquals("1.alpha1", summary.version());
        assertTrue(summary.supportedOperations().contains(AcpOperation.HELLO));
        assertTrue(summary.supportedOperations().contains(AcpOperation.ATTACH));
    }

    @Test
    void providesDeliveryRetryAndDeadLetterScaffolding() throws Exception {
        var planner = new DeliveryRetryPlanner(new DeliveryRetryPolicy(4, 250));
        assertTrue(planner.shouldRetry(2));
        assertFalse(planner.shouldRetry(4));
        assertEquals(750L, planner.nextDelayMillis(2));

        var temp = Files.createTempDirectory("binbun-dead-letter");
        var repo = new JsonDeliveryDeadLetterRepository(temp);
        repo.save(new DeliveryDeadLetterRecord("telegram", "idem-9", "session-9", "exhausted retries", null));
        assertEquals(1, repo.list().size());
    }

    @Test
    void validatesPluginEnvironmentRequirements() {
        var manifest = new PluginManifest("plugin-a", "0.1.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of("API_KEY", "BOT_TOKEN"), List.of(), "phase-2");
        var missing = new PluginEnvironmentValidator().missingRequiredEnv(manifest, Map.of("API_KEY", "set"));
        assertEquals(List.of("BOT_TOKEN"), missing);
    }

    @Test
    void producesGatewayOperationalSnapshot() {
        var snapshot = new GatewayOperationalSnapshotService(new GatewayHealthService(), new GatewayRecoveryCoordinator())
                .snapshot(true, true, false);
        assertEquals("UP", snapshot.health().liveness());
        assertTrue(snapshot.plannedRecoverySteps() > 0);
        assertFalse(snapshot.pluginRuntimeReady());
    }

    @Test
    void installsExpandedBrowserToolSet() {
        var tools = new ToolRegistry();
        new BrowserToolInstaller(new BrowserToolBridge(new PlaywrightBrowserService())).installInto(tools);
        assertEquals(4, tools.specs().stream().filter(spec -> spec.name().startsWith("browser.")).count());
    }
}
