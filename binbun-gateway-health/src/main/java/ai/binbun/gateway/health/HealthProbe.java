package ai.binbun.gateway.health;

public interface HealthProbe {
    String subsystemName();
    SubsystemHealth probe();
}
