package ai.binbun.deploy;

import java.util.List;

public final class VllmCommandBuilder {
    public List<String> installCommands() {
        return List.of(
                "python3 -m venv .venv",
                ". .venv/bin/activate && pip install --upgrade pip",
                ". .venv/bin/activate && pip install vllm"
        );
    }

    public String launchCommand(DeploymentSpec spec) {
        return ". .venv/bin/activate && python -m vllm.entrypoints.openai.api_server" +
                " --model " + spec.model() +
                " --host 0.0.0.0 --port " + spec.port() +
                " --tensor-parallel-size " + Math.max(1, spec.gpuCount());
    }

    public String stopCommand(int port) {
        return "pkill -f 'vllm.entrypoints.openai.api_server --host 0.0.0.0 --port " + port + "' || true";
    }

    public String logsCommand(int port) {
        return "tail -n 200 vllm-" + port + ".log";
    }
}
