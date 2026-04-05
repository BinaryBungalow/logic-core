package ai.binbun.gateway.recovery;

import ai.binbun.workflows.WorkflowRunRepository;

public final class WorkflowRecoveryExecutor implements RecoveryExecutor {
    private final WorkflowRunRepository workflowRunRepository;

    public WorkflowRecoveryExecutor(WorkflowRunRepository workflowRunRepository) {
        this.workflowRunRepository = workflowRunRepository;
    }

    @Override
    public String subsystemName() {
        return "workflows";
    }

    @Override
    public RecoveryCheckpoint execute() {
        if (workflowRunRepository == null) {
            return new RecoveryCheckpoint("workflows", "SKIPPED", "workflow repository not available");
        }
        try {
            var runs = workflowRunRepository.list().stream()
                    .filter(r -> r.status().equals("PENDING") || r.status().equals("RUNNING"))
                    .toList();
            return new RecoveryCheckpoint("workflows", "RECOVERED", "found " + runs.size() + " resumable workflow run(s)");
        } catch (Exception e) {
            return new RecoveryCheckpoint("workflows", "FAILED", "workflow recovery error: " + e.getMessage());
        }
    }
}
