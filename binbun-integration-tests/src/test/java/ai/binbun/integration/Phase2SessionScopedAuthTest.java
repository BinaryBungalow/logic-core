package ai.binbun.integration;

import ai.binbun.acp.auth.SessionTokenAuthenticator;
import ai.binbun.acp.auth.SessionTokenManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Phase2SessionScopedAuthTest {
    @Test
    void sessionTokenManagerGeneratesAndValidatesTokens() {
        var manager = new SessionTokenManager(5000);
        var token = manager.generateToken("user-1", "operator", "session-1");

        assertTrue(manager.validate(token.token()));
        assertEquals("user-1", manager.getToken(token.token()).subject());
        assertEquals("session-1", manager.getToken(token.token()).sessionId());
    }

    @Test
    void sessionTokenExpiresAfterTtl() throws InterruptedException {
        var manager = new SessionTokenManager(100);
        var token = manager.generateToken("user-1", "operator", "session-1");

        assertTrue(manager.validate(token.token()));
        Thread.sleep(150);
        assertFalse(manager.validate(token.token()));
    }

    @Test
    void sessionTokenAuthenticatorValidatesTokens() {
        var manager = new SessionTokenManager();
        var token = manager.generateToken("user-1", "operator", "session-1");
        var authenticator = new SessionTokenAuthenticator(manager);

        var result = authenticator.authenticateWithResult(token.token());
        assertTrue(result.isPresent());
        assertTrue(result.get().authenticated());
        assertEquals("user-1", result.get().principal().subject());
    }

    @Test
    void sessionTokenCanBeRevoked() {
        var manager = new SessionTokenManager();
        var token = manager.generateToken("user-1", "operator", "session-1");

        assertTrue(manager.validate(token.token()));
        manager.revoke(token.token());
        assertFalse(manager.validate(token.token()));
    }

    @Test
    void revokeBySessionRevokesAllTokensForSession() {
        var manager = new SessionTokenManager();
        var token1 = manager.generateToken("user-1", "operator", "session-1");
        var token2 = manager.generateToken("user-2", "node", "session-1");

        manager.revokeBySession("session-1");
        assertFalse(manager.validate(token1.token()));
        assertFalse(manager.validate(token2.token()));
    }
}
