package ai.binbun.plugin.resolver;

import ai.binbun.plugin.manifest.PluginDependency;
import ai.binbun.plugin.manifest.PluginManifest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class PluginResolver {
    private final PluginDeterministicResolver deterministicResolver = new PluginDeterministicResolver();

    public PluginLock resolve(List<PluginManifest> manifests) {
        Map<String, List<PluginManifest>> byName = new LinkedHashMap<>();
        for (PluginManifest m : manifests) {
            byName.computeIfAbsent(m.name(), k -> new ArrayList<>()).add(m);
        }

        List<ResolvedPlugin> resolved = new ArrayList<>();
        for (Map.Entry<String, List<PluginManifest>> entry : byName.entrySet()) {
            String name = entry.getKey();
            List<PluginManifest> versions = entry.getValue();

            if (versions.size() > 1) {
                throw new IllegalArgumentException("Dependency conflict for plugin: " + name + " has versions " +
                        versions.stream().map(PluginManifest::version).toList());
            }

            PluginManifest manifest = versions.get(0);
            String checksum = computeChecksum(manifest);
            resolved.add(new ResolvedPlugin(name, manifest.version(), checksum));

            for (PluginDependency dep : manifest.dependencies()) {
                VersionRange range = VersionRange.parse(dep.versionRange());
                boolean found = manifests.stream()
                        .filter(m -> m.name().equals(dep.name()))
                        .anyMatch(m -> range.matches(SemVer.parse(m.version())));
                if (!found && !dep.optional()) {
                    throw new IllegalArgumentException("Missing required dependency: " + dep.name() +
                            " (required: " + dep.versionRange() + ") for plugin: " + name);
                }
            }
        }

        return new PluginLock(resolved);
    }

    public PluginLockfile resolveLockfile(List<PluginManifest> manifests) {
        return deterministicResolver.resolveLockfile(manifests);
    }

    private String computeChecksum(PluginManifest manifest) {
        try {
            String input = manifest.name() + ":" + manifest.version() + ":" + manifest.entrypoint();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return manifest.name() + "-" + manifest.version();
        }
    }
}
