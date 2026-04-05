package ai.binbun.acp.auth;

import java.util.Set;

public record AcpPrincipal(String subject, String role, String deviceId, Set<String> scopes, Set<String> caps) {
    public AcpPrincipal(String subject, String role) {
        this(subject, role, null, Set.of(), Set.of());
    }

    public AcpPrincipal(String subject, String role, String deviceId, Set<String> scopes) {
        this(subject, role, deviceId, scopes, Set.of());
    }
}
