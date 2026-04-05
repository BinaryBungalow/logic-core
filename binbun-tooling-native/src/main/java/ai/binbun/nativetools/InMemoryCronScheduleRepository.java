package ai.binbun.nativetools;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryCronScheduleRepository implements CronScheduleRepository {
    private final List<CronSchedule> schedules = new ArrayList<>();

    @Override
    public void save(CronSchedule schedule) {
        schedules.add(schedule);
    }

    @Override
    public List<CronSchedule> list() {
        return List.copyOf(schedules);
    }
}
