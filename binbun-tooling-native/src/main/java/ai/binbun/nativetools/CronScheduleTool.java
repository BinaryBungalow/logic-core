package ai.binbun.nativetools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.model.ToolSpec;
import ai.binbun.tools.Tool;

import java.util.UUID;

public final class CronScheduleTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final CronScheduleRepository repository;

    public CronScheduleTool(CronScheduleRepository repository) {
        this.repository = repository;
    }

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        var properties = schema.putObject("properties");
        properties.putObject("expression").put("type", "string");
        properties.putObject("task").put("type", "string");
        properties.putObject("mode").put("type", "string");
        var required = schema.putArray("required");
        required.add("mode");
        schema.put("additionalProperties", false);
        return new ToolSpec("cron.schedule", "Store or inspect scheduled jobs.", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        String mode = arguments.path("mode").asText("list");
        try {
            if ("create".equals(mode)) {
                CronSchedule schedule = new CronSchedule(
                        UUID.randomUUID().toString(),
                        arguments.path("expression").asText(""),
                        arguments.path("task").asText("")
                );
                repository.save(schedule);
                return MAPPER.writeValueAsString(schedule);
            }
            return MAPPER.writeValueAsString(repository.list());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to handle cron request", e);
        }
    }
}
