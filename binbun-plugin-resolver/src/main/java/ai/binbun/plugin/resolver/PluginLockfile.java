package ai.binbun.plugin.resolver;

import java.util.List;

public record PluginLockfile(List<PluginLockfileEntry> entries) {
    public PluginLockfile {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
