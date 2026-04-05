package ai.binbun.plugin.runtime;

public interface PluginLifecycleHook {
    default void onActivate(String pluginName) {}
    default void onDeactivate(String pluginName) {}
    default void onUpgrade(String pluginName, String oldVersion, String newVersion) {}
    default void onMigration(String pluginName, String migrationName) {}
}
