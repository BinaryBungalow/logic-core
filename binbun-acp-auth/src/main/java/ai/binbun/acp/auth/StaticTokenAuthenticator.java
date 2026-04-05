package ai.binbun.acp.auth;

import java.util.Optional;

public final class StaticTokenAuthenticator implements AcpAuthenticator {
    private final String expectedToken;

    public StaticTokenAuthenticator(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public Optional<AcpPrincipal> authenticate(String token) {
        if (expectedToken != null && !expectedToken.isBlank() && expectedToken.equals(token)) {
            return Optional.of(new AcpPrincipal("subject", "client"));
        }
        return Optional.empty();
    }
}
