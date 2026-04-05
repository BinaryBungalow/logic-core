package ai.binbun.integration;

import ai.binbun.plugin.manifest.PluginDependency;
import ai.binbun.plugin.manifest.PluginManifest;
import ai.binbun.plugin.registry.PluginRegistryService;
import ai.binbun.plugin.resolver.PluginResolver;
import ai.binbun.plugin.resolver.SemVer;
import ai.binbun.plugin.resolver.VersionRange;
import ai.binbun.plugin.runtime.PluginLifecycleHook;
import ai.binbun.plugin.runtime.PluginLifecycleState;
import ai.binbun.plugin.runtime.PluginRuntimeManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase2PluginLifecycleTest {
    @Test
    void semVerParsesVersionsCorrectly() {
        assertEquals(0, SemVer.parse("1.0.0").compareTo(SemVer.parse("1.0.0")));
        assertTrue(SemVer.parse("2.0.0").compareTo(SemVer.parse("1.0.0")) > 0);
        assertTrue(SemVer.parse("1.1.0").compareTo(SemVer.parse("1.0.0")) > 0);
        assertTrue(SemVer.parse("1.0.1").compareTo(SemVer.parse("1.0.0")) > 0);
    }

    @Test
    void versionRangeMatchesSemverConstraints() {
        assertTrue(VersionRange.parse(">=1.0.0").matches(SemVer.parse("1.0.0")));
        assertTrue(VersionRange.parse(">=1.0.0").matches(SemVer.parse("2.0.0")));
        assertFalse(VersionRange.parse(">=1.0.0").matches(SemVer.parse("0.9.0")));
        assertTrue(VersionRange.parse(">=1.0.0 <2.0.0").matches(SemVer.parse("1.5.0")));
        assertFalse(VersionRange.parse(">=1.0.0 <2.0.0").matches(SemVer.parse("2.0.0")));
    }

    @Test
    void pluginResolverDetectsConflicts() {
        var resolver = new PluginResolver();
        var manifests = List.of(
                new PluginManifest("plugin-a", "1.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "0.1.0"),
                new PluginManifest("plugin-a", "2.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "0.1.0")
        );

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(manifests));
    }

    @Test
    void pluginResolverValidatesDependencies() {
        var resolver = new PluginResolver();
        var deps = List.of(new PluginDependency("missing-dep", ">=1.0.0", false));
        var manifests = List.of(
                new PluginManifest("plugin-a", "1.0.0", "entry", deps, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "0.1.0")
        );

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(manifests));
    }

    @Test
    void pluginRuntimeManagerEnforcesLifecycle() {
        var manager = new PluginRuntimeManager();
        var manifest = new PluginManifest("test-plugin", "1.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "0.1.0");

        var installed = manager.install(manifest);
        assertEquals(PluginLifecycleState.INSTALLED, installed.state());

        var activated = manager.activate("test-plugin");
        assertEquals(PluginLifecycleState.ACTIVATED, activated.state());

        var deactivated = manager.deactivate("test-plugin");
        assertEquals(PluginLifecycleState.DEACTIVATED, deactivated.state());

        assertEquals(1, manager.list().size());
    }

    @Test
    void pluginRegistryDiscoversFromDirectory() throws Exception {
        var registry = new PluginRegistryService();
        assertEquals(1, registry.listBundled().size());

        Path tempDir = Files.createTempDirectory("plugin-discovery");
        var discovered = registry.discoverFrom(tempDir);
        assertTrue(discovered.isEmpty());

        Files.deleteIfExists(tempDir);
    }

    @Test
    void pluginLifecycleHooksAreCalled() {
        var hookCalled = new java.util.concurrent.atomic.AtomicBoolean(false);
        var hook = new PluginLifecycleHook() {
            @Override public void onActivate(String pluginName) { hookCalled.set(true); }
        };
        var manager = new PluginRuntimeManager(List.of(hook));
        var manifest = new PluginManifest("hook-test", "1.0.0", "entry", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "0.1.0");

        manager.install(manifest);
        manager.activate("hook-test");

        assertTrue(hookCalled.get());
    }
}
