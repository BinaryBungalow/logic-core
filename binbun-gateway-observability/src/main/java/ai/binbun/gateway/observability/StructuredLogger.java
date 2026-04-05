package ai.binbun.gateway.observability;

import java.io.PrintStream;
import java.time.Instant;
import java.util.Map;

public final class StructuredLogger {
    private final PrintStream out;
    private final String component;

    public StructuredLogger(PrintStream out, String component) {
        this.out = out;
        this.component = component;
    }

    public StructuredLogger(String component) {
        this(System.err, component);
    }

    public void info(String message, Map<String, String> fields) {
        log("INFO", message, fields);
    }

    public void warn(String message, Map<String, String> fields) {
        log("WARN", message, fields);
    }

    public void error(String message, Map<String, String> fields) {
        log("ERROR", message, fields);
    }

    public void info(String message) {
        info(message, Map.of());
    }

    public void warn(String message) {
        warn(message, Map.of());
    }

    public void error(String message) {
        error(message, Map.of());
    }

    private void log(String level, String message, Map<String, String> fields) {
        var sb = new StringBuilder();
        sb.append("{\"timestamp\":\"").append(Instant.now()).append("\"");
        sb.append(",\"level\":\"").append(level).append("\"");
        sb.append(",\"component\":\"").append(component).append("\"");
        sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");

        String correlationId = CorrelationContext.correlationId();
        if (correlationId != null) {
            sb.append(",\"correlationId\":\"").append(escapeJson(correlationId)).append("\"");
        }
        String sessionId = CorrelationContext.sessionId();
        if (sessionId != null) {
            sb.append(",\"sessionId\":\"").append(escapeJson(sessionId)).append("\"");
        }

        for (var entry : fields.entrySet()) {
            sb.append(",\"").append(escapeJson(entry.getKey())).append("\":\"").append(escapeJson(entry.getValue())).append("\"");
        }
        sb.append("}");
        out.println(sb);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
