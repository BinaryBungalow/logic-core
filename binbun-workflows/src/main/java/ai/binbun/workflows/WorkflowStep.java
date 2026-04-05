package ai.binbun.workflows;

public record WorkflowStep(String id, String type, String input, boolean requiresApproval) {
}
