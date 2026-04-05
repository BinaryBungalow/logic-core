package ai.binbun.deploy;

public interface PodManager {
    String createPod(String spec);
    String deployModel(String podId, String model);
}
