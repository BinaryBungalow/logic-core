package ai.binbun.deploy;

public record DeploymentHandle(String id, String provider, String endpoint, String status) {}
