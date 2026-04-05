package ai.binbun.deploy;

public record SshTarget(String host, String user, int port, String workdir) {
    public String authority() {
        return user + "@" + host;
    }
}
