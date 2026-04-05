package ai.binbun.workflows;

import java.util.UUID;

public final class WorkflowEngine {
    private final WorkflowRunRepository repository;

    public WorkflowEngine(WorkflowRunRepository repository) {
        this.repository = repository;
    }

    public WorkflowRunState start(WorkflowDefinition definition) {
        WorkflowRunState state;
        if (definition.steps().isEmpty()) {
            state = new WorkflowRunState(definition.name(), 0, "COMPLETED", UUID.randomUUID().toString(), false);
        } else {
            WorkflowStep first = definition.steps().get(0);
            state = first.requiresApproval()
                    ? new WorkflowRunState(definition.name(), 0, "NEEDS_APPROVAL", UUID.randomUUID().toString(), true)
                    : new WorkflowRunState(definition.name(), 1, "RUNNING", UUID.randomUUID().toString(), false);
        }
        repository.save(state);
        return state;
    }

    public WorkflowRunState approve(WorkflowDefinition definition, String resumeToken) {
        WorkflowRunState prior = repository.find(resumeToken)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow run: " + resumeToken));
        if (!prior.approvalRequired()) {
            return prior;
        }
        int nextStep = Math.min(prior.stepIndex() + 1, definition.steps().size());
        String status = nextStep >= definition.steps().size() ? "COMPLETED" : "RUNNING";
        WorkflowRunState updated = new WorkflowRunState(definition.name(), nextStep, status, prior.resumeToken(), false);
        repository.save(updated);
        return updated;
    }

    public WorkflowRunState resume(WorkflowDefinition definition, String resumeToken) {
        WorkflowRunState prior = repository.find(resumeToken)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow run: " + resumeToken));
        if (prior.approvalRequired()) {
            return prior;
        }
        int nextStep = Math.min(prior.stepIndex() + 1, definition.steps().size());
        String status = nextStep >= definition.steps().size() ? "COMPLETED" : "RUNNING";
        WorkflowRunState updated = new WorkflowRunState(definition.name(), nextStep, status, prior.resumeToken(), false);
        repository.save(updated);
        return updated;
    }
}
