package ai.binbun.nativetools;

import java.time.Instant;
import java.util.Optional;

public interface CronExecutionStateRepository {
    Optional<Instant> lastRunAt(String scheduleId);
    void markExecuted(String scheduleId, Instant ranAt);
}
