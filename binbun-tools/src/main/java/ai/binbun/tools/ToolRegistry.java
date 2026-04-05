package ai.binbun.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.model.ToolCall;
import ai.binbun.model.ToolSpec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public final class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolRegistry register(Tool tool) {
        tools.put(tool.spec().name(), tool);
        return this;
    }

    public boolean isEmpty() {
        return tools.isEmpty();
    }

    public List<ToolSpec> specs() {
        return tools.values().stream().map(Tool::spec).toList();
    }

    public Collection<Tool> all() {
        return List.copyOf(tools.values());
    }

    public String execute(ToolCall call) {
        Tool tool = tools.get(call.name());
        if (tool == null) {
            throw new NoSuchElementException("Unknown tool: " + call.name());
        }
        JsonNode arguments = parse(call.argumentsJson());
        validate(tool.spec().parameters(), arguments, "$", true);
        return tool.execute(arguments);
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void validate(JsonNode schema, JsonNode value, String path, boolean root) {
        if (schema == null || schema.isMissingNode() || schema.isNull()) {
            return;
        }

        JsonNode enumValues = schema.path("enum");
        if (enumValues.isArray() && enumValues.size() > 0) {
            boolean matched = false;
            for (JsonNode enumValue : enumValues) {
                if (enumValue.equals(value)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new IllegalArgumentException("Value at " + path + " is not in enum");
            }
        }

        String type = schema.path("type").asText("");
        if (!type.isBlank() && !matchesType(type, value)) {
            throw new IllegalArgumentException("Invalid type at " + path + ": expected " + type);
        }

        if ("object".equals(type)) {
            JsonNode required = schema.path("required");
            if (required.isArray()) {
                for (JsonNode name : required) {
                    String field = name.asText();
                    if (!value.has(field)) {
                        throw new IllegalArgumentException("Missing required field at " + path + ": " + field);
                    }
                }
            }

            JsonNode properties = schema.path("properties");
            if (properties.isObject()) {
                properties.fields().forEachRemaining(entry -> {
                    JsonNode child = value.get(entry.getKey());
                    if (child != null) {
                        validate(entry.getValue(), child, path + "." + entry.getKey(), false);
                    }
                });
            }

            JsonNode additionalProperties = schema.get("additionalProperties");
            if (additionalProperties != null && additionalProperties.isBoolean() && !additionalProperties.asBoolean()) {
                value.fieldNames().forEachRemaining(field -> {
                    if (!schema.path("properties").has(field)) {
                        throw new IllegalArgumentException("Unexpected field at " + path + ": " + field);
                    }
                });
            }
        }

        if ("array".equals(type) && value.isArray()) {
            JsonNode items = schema.path("items");
            for (int i = 0; i < value.size(); i++) {
                validate(items, value.get(i), path + "[" + i + "]", false);
            }
        }

        if (root && !value.isObject() && "object".equals(type)) {
            throw new IllegalArgumentException("Tool arguments must be a JSON object");
        }
    }

    private boolean matchesType(String type, JsonNode value) {
        return switch (type) {
            case "string" -> value.isTextual();
            case "number" -> value.isNumber();
            case "integer" -> value.isIntegralNumber();
            case "boolean" -> value.isBoolean();
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "null" -> value.isNull();
            default -> true;
        };
    }
}
