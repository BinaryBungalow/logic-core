package ai.binbun.tools;

import com.fasterxml.jackson.databind.JsonNode;
import ai.binbun.model.ToolSpec;

public interface Tool {
    ToolSpec spec();
    String execute(JsonNode arguments);
}
