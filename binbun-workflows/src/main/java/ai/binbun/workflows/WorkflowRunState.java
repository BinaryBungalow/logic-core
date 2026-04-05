package ai.binbun.workflows;

public record WorkflowRunState(String workflowName, int stepIndex, String status, String resumeToken, boolean approvalRequired) {
}
