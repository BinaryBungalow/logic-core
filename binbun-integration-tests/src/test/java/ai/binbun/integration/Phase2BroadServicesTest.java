package ai.binbun.integration;

import ai.binbun.delivery.core.DeliveryFailureHandler;
import ai.binbun.delivery.core.DeliveryRetryPlanner;
import ai.binbun.delivery.core.DeliveryRetryPolicy;
import ai.binbun.delivery.model.JsonDeliveryDeadLetterRepository;
import ai.binbun.plugin.manifest.PluginEnvironmentReportService;
import ai.binbun.plugin.manifest.PluginManifest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2BroadServicesTest {
    @Test
    void createsPluginEnvironmentReport() {
        var manifest = new PluginManifest("plugin-b", "0.1.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of("API_KEY", "TOKEN"), List.of(), "phase-2");
        var report = new PluginEnvironmentReportService().evaluate(manifest, Map.of("API_KEY", "set"));
        assertFalse(report.ready());
        assertEquals(List.of("TOKEN"), report.missingRequiredEnv());
    }

    @Test
    void convertsExhaustedFailuresIntoDeadLetters() throws Exception {
        var repo = new JsonDeliveryDeadLetterRepository(Files.createTempDirectory("binbun-broad-services"));
        var handler = new DeliveryFailureHandler(new DeliveryRetryPlanner(new DeliveryRetryPolicy(2, 500)), repo);
        var retryDecision = handler.onFailure("telegram", "idem-retry", "session-1", "temporary", 1);
        var deadLetterDecision = handler.onFailure("telegram", "idem-dead", "session-1", "permanent", 2);
        assertTrue(retryDecision.retryable());
        assertEquals(1000L, retryDecision.nextDelayMillis());
        assertFalse(deadLetterDecision.retryable());
        assertTrue(deadLetterDecision.deadLettered());
        assertEquals(1, repo.list().size());
    }
}
