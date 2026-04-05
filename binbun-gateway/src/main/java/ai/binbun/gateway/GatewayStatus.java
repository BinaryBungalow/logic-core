package ai.binbun.gateway;

public record GatewayStatus(boolean running, String startedAt, int sessionCount) {
}
