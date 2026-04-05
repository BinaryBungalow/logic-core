package ai.binbun.acp.auth;

import java.util.Optional;

public final class SessionTokenAuthenticator implements AcpAuthenticator {
    private final SessionTokenManager tokenManager;

    public SessionTokenAuthenticator(SessionTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Optional<AcpPrincipal> authenticate(String token) {
        if (tokenManager.validate(token)) {
            SessionTokenManager.SessionToken st = tokenManager.getToken(token);
            return Optional.of(new AcpPrincipal(st.subject(), st.role()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<AcpAuthResult> authenticateWithResult(String token) {
        if (tokenManager.validate(token)) {
            SessionTokenManager.SessionToken st = tokenManager.getToken(token);
            return Optional.of(new AcpAuthResult(true, new AcpPrincipal(st.subject(), st.role()), null, null));
        }
        return Optional.of(new AcpAuthResult(false, null, "INVALID_TOKEN", "Session token is invalid or expired"));
    }

    public SessionTokenManager tokenManager() {
        return tokenManager;
    }
}
