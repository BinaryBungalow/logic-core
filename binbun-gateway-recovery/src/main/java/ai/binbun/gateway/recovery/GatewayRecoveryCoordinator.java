package ai.binbun.gateway.recovery;

import java.util.List;

public final class GatewayRecoveryCoordinator {
    private final List<RecoveryExecutor> executors;

    public GatewayRecoveryCoordinator(List<RecoveryExecutor> executors) {
        this.executors = executors == null ? List.of() : List.copyOf(executors);
    }

    public GatewayRecoveryCoordinator() {
        this(List.of());
    }

    public List<RecoveryCheckpoint> startupPlan() {
        return List.of(
                new RecoveryCheckpoint("sessions", "PLANNED", "rehydrate checkpointed sessions"),
                new RecoveryCheckpoint("workflows", "PLANNED", "resume persisted workflow runs"),
                new RecoveryCheckpoint("delivery", "PLANNED", "replay queued delivery jobs"),
                new RecoveryCheckpoint("plugins", "PLANNED", "reactivate installed plugins")
        );
    }

    public List<RecoveryCheckpoint> executeRecoveryPlan() {
        return executors.stream()
                .map(RecoveryExecutor::execute)
                .toList();
    }
}
