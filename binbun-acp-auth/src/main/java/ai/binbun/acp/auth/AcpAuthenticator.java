package ai.binbun.acp.auth;

import java.util.Optional;

public interface AcpAuthenticator {
    Optional<AcpPrincipal> authenticate(String token);

    default Optional<AcpAuthResult> authenticateWithResult(String token) {
        return authenticate(token).map(p -> new AcpAuthResult(true, p, null, null));
    }
}
