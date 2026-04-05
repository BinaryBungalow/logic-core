package ai.binbun.integration;

import ai.binbun.acp.protocol.AcpProtocolSummaryService;
import ai.binbun.browser.core.BrowserToolBridge;
import ai.binbun.browser.core.BrowserToolInstaller;
import ai.binbun.browser.playwright.PlaywrightBrowserService;
import ai.binbun.delivery.core.ConnectorRegistry;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2BalancedGatewayRuntimeTest {
    @Test
    void wiresBalancedPhase2ScaffoldingIntoRuntimeFacingSurfaces() throws Exception {
        var tools = new ToolRegistry();
        var browserBridge = new BrowserToolBridge(new PlaywrightBrowserService());
        new BrowserToolInstaller(browserBridge).installInto(tools);

        var connectors = new ConnectorRegistry();
        var retryPlanner = new DeliveryRetryPlanner(new DeliveryRetryPolicy(3, 1000));
        var deadLetters = new JsonDeliveryDeadLetterRepository(Files.createTempDirectory("binbun-balanced-runtime"));
        deadLetters.save(new DeliveryDeadLetterRecord("telegram", "idem-runtime", "session-runtime", "starter exhausted retries", Instant.now()));

        var snapshot = new GatewayOperationalSnapshotService(new GatewayHealthService(), new GatewayRecoveryCoordinator())
                .snapshot(true, !connectors.all().isEmpty(), false);
        var protocolSummary = new AcpProtocolSummaryService();
        var missingEnv = new PluginEnvironmentValidator().missingRequiredEnv(
                new PluginManifest("plugin-a", "0.1.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of("API_KEY"), List.of(), "phase-2"),
                Map.of()
        );

        assertEquals("1.alpha1", protocolSummary.version());
        assertTrue(protocolSummary.supportedOperations().size() > 3);
        assertEquals(4, tools.specs().stream().filter(spec -> spec.name().startsWith("browser.")).count());
        assertTrue(retryPlanner.shouldRetry(1));
        assertEquals(1, deadLetters.list().size());
        assertEquals("UP", snapshot.health().liveness());
        assertEquals(List.of("API_KEY"), missingEnv);
    }
}
