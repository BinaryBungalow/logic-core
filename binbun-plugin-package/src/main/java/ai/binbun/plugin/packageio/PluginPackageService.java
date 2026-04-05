package ai.binbun.plugin.packageio;

public final class PluginPackageService {
    public PluginPackageDescriptor describe(String packageName) {
        return new PluginPackageDescriptor(packageName, "checksum-placeholder", false);
    }
}
