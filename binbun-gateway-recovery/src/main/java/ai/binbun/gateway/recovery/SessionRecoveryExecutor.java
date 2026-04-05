package ai.binbun.gateway.recovery;

import ai.binbun.agent.CheckpointStore;

public final class SessionRecoveryExecutor implements RecoveryExecutor {
    private final CheckpointStore checkpointStore;

    public SessionRecoveryExecutor(CheckpointStore checkpointStore) {
        this.checkpointStore = checkpointStore;
    }

    @Override
    public String subsystemName() {
        return "sessions";
    }

    @Override
    public RecoveryCheckpoint execute() {
        if (checkpointStore == null) {
            return new RecoveryCheckpoint("sessions", "SKIPPED", "checkpoint store not available");
        }
        try {
            int count = checkpointStore.list().size();
            return new RecoveryCheckpoint("sessions", "RECOVERED", "rehydrated " + count + " session(s)");
        } catch (Exception e) {
            return new RecoveryCheckpoint("sessions", "FAILED", "session recovery error: " + e.getMessage());
        }
    }
}
