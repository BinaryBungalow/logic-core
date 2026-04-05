package ai.binbun.acp.auth;

public final class AcpAuthService {
    private final AcpAuthenticator authenticator;

    public AcpAuthService(AcpAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    public AcpAuthResult authenticate(String token) {
        return authenticator.authenticate(token)
                .map(AcpAuthResult::success)
                .orElseGet(() -> AcpAuthResult.failure("auth_failed", "Invalid ACP token"));
    }
}
