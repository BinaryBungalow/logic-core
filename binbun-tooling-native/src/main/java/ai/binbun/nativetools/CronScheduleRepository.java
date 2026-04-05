package ai.binbun.nativetools;

import java.util.List;

public interface CronScheduleRepository {
    void save(CronSchedule schedule);
    List<CronSchedule> list();
}
