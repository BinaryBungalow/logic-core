package ai.binbun.gateway;

import java.util.Map;
import java.util.Set;

public record GatewayMethod(
        String name,
        Set<OperatorScope> requiredScopes,
        GatewayMethodHandler handler
) {
    public interface GatewayMethodHandler {
        Map<String, Object> handle(Map<String, Object> params, GatewayConnection connection);
    }
}
