package ai.binbun.gateway.recovery;

public interface RecoveryExecutor {
    String subsystemName();
    RecoveryCheckpoint execute();
}
