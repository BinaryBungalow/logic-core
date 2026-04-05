package ai.binbun.nativetools;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonCronScheduleRepository implements CronScheduleRepository {
    private final Path root;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonCronScheduleRepository(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void save(CronSchedule schedule) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve(schedule.id() + ".json").toFile(), schedule);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<CronSchedule> list() {
        try {
            List<CronSchedule> schedules = new ArrayList<>();
            try (var stream = Files.list(root)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                schedules.add(objectMapper.readValue(path.toFile(), CronSchedule.class));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
            return schedules;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
