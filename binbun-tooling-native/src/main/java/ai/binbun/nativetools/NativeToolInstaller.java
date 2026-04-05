package ai.binbun.nativetools;

import ai.binbun.gateway.GatewayRuntime;
import ai.binbun.tools.ToolRegistry;

public final class NativeToolInstaller {
    private final GatewayRuntime gatewayRuntime;
    private final MessageDispatcher messageDispatcher;
    private final CronScheduleRepository cronSchedules;

    public NativeToolInstaller(GatewayRuntime gatewayRuntime, MessageDispatcher messageDispatcher, CronScheduleRepository cronSchedules) {
        this.gatewayRuntime = gatewayRuntime;
        this.messageDispatcher = messageDispatcher;
        this.cronSchedules = cronSchedules;
    }

    public ToolRegistry installInto(ToolRegistry registry) {
        registry.register(new GatewayStatusTool(gatewayRuntime));
        registry.register(new SessionsListTool(gatewayRuntime));
        registry.register(new MessageSendTool(messageDispatcher));
        registry.register(new CronScheduleTool(cronSchedules));
        return registry;
    }
}
