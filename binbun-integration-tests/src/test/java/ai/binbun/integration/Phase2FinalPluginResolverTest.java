package ai.binbun.integration;

import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.manifest.PluginPermission;
import ai.binbun.plugin.resolver.PluginDeterministicResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Phase2FinalPluginResolverTest {
    @Test
    void resolverProducesDeterministicLockfileAndDetectsConflicts() {
        var resolver = new PluginDeterministicResolver();
        var a = new PluginManifest("alpha", "1.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(PluginPermission.BROWSER), List.of(), List.of(), "phase-2");
        var b = new PluginManifest("beta", "2.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "phase-2");
        var lockfile = resolver.resolveLockfile(List.of(b, a));
        assertEquals("alpha", lockfile.entries().get(0).name());
        assertEquals("beta", lockfile.entries().get(1).name());
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveLockfile(List.of(a, new PluginManifest("alpha", "2.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "phase-2"))));
    }
}
