package ai.binbun.plugin.resolver;

import java.util.List;

public record PluginLock(List<ResolvedPlugin> plugins) {
    public PluginLock {
        plugins = plugins == null ? List.of() : List.copyOf(plugins);
    }
}
