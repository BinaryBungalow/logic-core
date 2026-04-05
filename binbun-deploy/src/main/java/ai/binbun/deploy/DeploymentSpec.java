package ai.binbun.deploy;

public record DeploymentSpec(String provider, String model, int gpuCount, String accelerator, int port) {}
