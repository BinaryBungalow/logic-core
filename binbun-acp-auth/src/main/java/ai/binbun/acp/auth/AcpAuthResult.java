package ai.binbun.acp.auth;

public record AcpAuthResult(boolean authenticated, AcpPrincipal principal, String errorCode, String errorMessage) {
    public static AcpAuthResult success(AcpPrincipal principal) {
        return new AcpAuthResult(true, principal, null, null);
    }

    public static AcpAuthResult failure(String code, String message) {
        return new AcpAuthResult(false, null, code, message);
    }
}
