package ai.binbun.nativetools;

import java.time.Instant;

public final class CronExecutor {
    private final CronScheduleRepository repository;
    private final MessageDispatcher dispatcher;
    private final CronExecutionStateRepository executionState;
    private final CronExpressionMatcher matcher;

    public CronExecutor(CronScheduleRepository repository, MessageDispatcher dispatcher,
                        CronExecutionStateRepository executionState, CronExpressionMatcher matcher) {
        this.repository = repository;
        this.dispatcher = dispatcher;
        this.executionState = executionState;
        this.matcher = matcher;
    }

    public int executeDueSchedules(Instant now) {
        int executed = 0;
        for (CronSchedule schedule : repository.list()) {
            Instant lastRunAt = executionState.lastRunAt(schedule.id()).orElse(null);
            if (matcher.matches(schedule.expression(), now, lastRunAt)) {
                dispatcher.dispatch(new MessageDispatch("local://cron", schedule.task(), "cron"));
                executionState.markExecuted(schedule.id(), now);
                executed++;
            }
        }
        return executed;
    }
}
