package ai.binbun.nativetools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.binbun.model.ToolSpec;
import ai.binbun.tools.Tool;

public final class MessageSendTool implements Tool {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final MessageDispatcher messageDispatcher;

    public MessageSendTool(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public ToolSpec spec() {
        var schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        var properties = schema.putObject("properties");
        properties.putObject("destination").put("type", "string");
        properties.putObject("body").put("type", "string");
        properties.putObject("channel").put("type", "string");
        var required = schema.putArray("required");
        required.add("destination");
        required.add("body");
        required.add("channel");
        schema.put("additionalProperties", false);
        return new ToolSpec("message.send", "Dispatch an outbound message via the configured sink.", schema);
    }

    @Override
    public String execute(JsonNode arguments) {
        return messageDispatcher.dispatch(new MessageDispatch(
                arguments.path("destination").asText(""),
                arguments.path("body").asText(""),
                arguments.path("channel").asText("default")
        ));
    }
}
