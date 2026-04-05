package ai.binbun.delivery.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.delivery.model.DeliveryDeadLetterRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonDeliveryDeadLetterRepository {
    private final Path root;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public JsonDeliveryDeadLetterRepository(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void save(DeliveryDeadLetterRecord record) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve(record.connector() + "-" + record.idempotencyKey() + ".json").toFile(), record);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<DeliveryDeadLetterRecord> list() {
        try {
            List<DeliveryDeadLetterRecord> results = new ArrayList<>();
            var files = Files.list(root).filter(path -> path.toString().endsWith(".json")).toList();
            for (Path file : files) {
                results.add(objectMapper.readValue(file.toFile(), DeliveryDeadLetterRecord.class));
            }
            return results;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
