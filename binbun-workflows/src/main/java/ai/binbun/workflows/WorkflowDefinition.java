package ai.binbun.workflows;

import java.util.List;

public record WorkflowDefinition(String name, List<WorkflowStep> steps) {
    public WorkflowDefinition {
        steps = List.copyOf(steps);
    }
}
