package ai.binbun.delivery.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.delivery.model.DeliveryJob;
import ai.binbun.delivery.model.DeliveryJobRepository;
import ai.binbun.delivery.model.DeliveryJobStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JsonDeliveryJobRepository implements DeliveryJobRepository {
    private final Path root;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public JsonDeliveryJobRepository(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void save(DeliveryJob job) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve(fileName(job.connector(), job.idempotencyKey())).toFile(), job);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<DeliveryJob> findByIdempotencyKey(String connector, String idempotencyKey) {
        Path file = root.resolve(fileName(connector, idempotencyKey));
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), DeliveryJob.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<DeliveryJob> list() {
        try {
            List<DeliveryJob> jobs = new ArrayList<>();
            var files = Files.list(root).filter(path -> path.toString().endsWith(".json")).toList();
            for (Path file : files) {
                jobs.add(objectMapper.readValue(file.toFile(), DeliveryJob.class));
            }
            return jobs;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<DeliveryJob> listPending() {
        return list().stream().filter(job -> job.status() == DeliveryJobStatus.PENDING).toList();
    }

    private String fileName(String connector, String idempotencyKey) {
        return connector + "-" + idempotencyKey + ".json";
    }
}
