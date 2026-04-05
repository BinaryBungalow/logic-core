package ai.binbun.deploy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SshDeploymentProvider implements DeploymentProvider {
    private final List<DeploymentHandle> handles = new ArrayList<>();
    private final VllmCommandBuilder commands = new VllmCommandBuilder();

    @Override
    public String name() {
        return "ssh-vllm";
    }

    @Override
    public DeploymentHandle plan(DeploymentSpec spec) {
        DeploymentHandle handle = new DeploymentHandle(UUID.randomUUID().toString(), name(),
                "http://pending:" + spec.port() + "/v1", "PLANNED");
        handles.add(handle);
        return handle;
    }

    @Override
    public DeploymentHandle apply(DeploymentSpec spec, SshTarget target) {
        String id = UUID.randomUUID().toString();
        runRemote(target, "mkdir -p " + target.workdir());
        for (String install : commands.installCommands()) {
            runRemote(target, "cd " + target.workdir() + " && " + install);
        }
        runRemote(target, "cd " + target.workdir() + " && nohup " + commands.launchCommand(spec) +
                " > vllm-" + spec.port() + ".log 2>&1 &");
        DeploymentHandle handle = new DeploymentHandle(id, name(),
                "http://" + target.host() + ":" + spec.port() + "/v1", "RUNNING");
        handles.add(handle);
        return handle;
    }

    @Override
    public List<DeploymentHandle> list() {
        return List.copyOf(handles);
    }

    @Override
    public DeploymentHandle stop(String id, SshTarget target) {
        DeploymentHandle handle = handles.stream().filter(h -> h.id().equals(id)).findFirst()
                .orElse(new DeploymentHandle(id, name(), "", "NOT_FOUND"));
        if ("NOT_FOUND".equals(handle.status())) {
            return handle;
        }
        int port = parsePort(handle.endpoint());
        runRemote(target, "cd " + target.workdir() + " && " + commands.stopCommand(port));
        DeploymentHandle stopped = new DeploymentHandle(handle.id(), handle.provider(), handle.endpoint(), "STOPPED");
        handles.removeIf(h -> h.id().equals(id));
        handles.add(stopped);
        return stopped;
    }

    @Override
    public String logs(String id, SshTarget target) {
        DeploymentHandle handle = handles.stream().filter(h -> h.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown deployment id: " + id));
        int port = parsePort(handle.endpoint());
        return runRemote(target, "cd " + target.workdir() + " && " + commands.logsCommand(port));
    }

    @Override
    public boolean health(String id, SshTarget target) {
        DeploymentHandle handle = handles.stream().filter(h -> h.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown deployment id: " + id));
        try {
            URI uri = URI.create(handle.endpoint().replace("/v1", "/health"));
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setRequestMethod("GET");
            return connection.getResponseCode() >= 200 && connection.getResponseCode() < 300;
        } catch (IOException e) {
            return false;
        }
    }

    private int parsePort(String endpoint) {
        int colon = endpoint.lastIndexOf(':');
        int slash = endpoint.indexOf('/', colon);
        return Integer.parseInt(endpoint.substring(colon + 1, slash > colon ? slash : endpoint.length()));
    }

    private String runRemote(SshTarget target, String command) {
        try {
            Process process = new ProcessBuilder("ssh", "-p", String.valueOf(target.port()), target.authority(), command)
                    .redirectErrorStream(true)
                    .start();
            byte[] out = process.getInputStream().readAllBytes();
            int code = process.waitFor();
            String text = new String(out, StandardCharsets.UTF_8);
            if (code != 0) {
                throw new IllegalStateException("SSH command failed: " + text);
            }
            return text;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running remote SSH command", e);
        }
    }
}
