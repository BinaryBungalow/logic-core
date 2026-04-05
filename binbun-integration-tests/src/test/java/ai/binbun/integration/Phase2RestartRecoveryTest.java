package ai.binbun.integration;

import ai.binbun.gateway.health.AcpHealthProbe;
import ai.binbun.gateway.health.DeliveryHealthProbe;
import ai.binbun.gateway.health.GatewayHealthService;
import ai.binbun.gateway.health.PluginHealthProbe;
import ai.binbun.gateway.recovery.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase2RestartRecoveryTest {
    @Test
    void recoveryCoordinatorExecutesPlan() {
        var coordinator = new GatewayRecoveryCoordinator(List.of(
                new SessionRecoveryExecutor(null),
                new WorkflowRecoveryExecutor(null),
                new DeliveryRecoveryExecutor(null),
                new PluginRecoveryExecutor(null, null)
        ));

        var results = coordinator.executeRecoveryPlan();
        assertEquals(4, results.size());
        assertTrue(results.stream().anyMatch(r -> r.subsystem().equals("sessions")));
        assertTrue(results.stream().anyMatch(r -> r.subsystem().equals("workflows")));
        assertTrue(results.stream().anyMatch(r -> r.subsystem().equals("delivery")));
        assertTrue(results.stream().anyMatch(r -> r.subsystem().equals("plugins")));
    }

    @Test
    void recoverySkipsWhenRepositoriesNull() {
        var sessionRecovery = new SessionRecoveryExecutor(null);
        var result = sessionRecovery.execute();
        assertEquals("SKIPPED", result.status());
    }

    @Test
    void healthProbeAggregatesDynamically() {
        var healthService = new GatewayHealthService(List.of(
                new AcpHealthProbe(null),
                new DeliveryHealthProbe(null),
                new PluginHealthProbe(null)
        ));

        var report = healthService.snapshot();
        assertEquals("UP", report.liveness());
        assertFalse(report.subsystems().isEmpty());
    }
}
