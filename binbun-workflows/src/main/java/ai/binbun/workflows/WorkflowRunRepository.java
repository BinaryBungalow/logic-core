package ai.binbun.workflows;

import java.util.List;
import java.util.Optional;

public interface WorkflowRunRepository {
    void save(WorkflowRunState state);
    Optional<WorkflowRunState> find(String resumeToken);
    List<WorkflowRunState> list();
}
