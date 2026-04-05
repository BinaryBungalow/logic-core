package ai.binbun.integration;

import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.runtime.PluginUpgradeManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase2FinalPluginUpgradeTest {
    @Test
    void failedUpgradeRollsBackToPreviousManifest() {
        var previous = new PluginManifest("core", "1.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "phase-2");
        var candidate = new PluginManifest("core", "1.1.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "phase-2");
        var manager = new PluginUpgradeManager();
        var failed = manager.upgrade(previous, candidate, true, true, true, false);
        var success = manager.upgrade(previous, candidate, true, true, true, true);
        assertFalse(failed.success());
        assertEquals(previous.version(), failed.activeManifest().version());
        assertTrue(success.success());
        assertEquals(candidate.version(), success.activeManifest().version());
    }
}
