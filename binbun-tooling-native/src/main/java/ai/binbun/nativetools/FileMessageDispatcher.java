package ai.binbun.nativetools;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public final class FileMessageDispatcher implements MessageDispatcher {
    private final Path outboxFile;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileMessageDispatcher(Path outboxFile) {
        this.outboxFile = outboxFile;
        try {
            Files.createDirectories(outboxFile.getParent());
            if (!Files.exists(outboxFile)) {
                Files.createFile(outboxFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String dispatch(MessageDispatch dispatch) {
        try {
            String line = objectMapper.writeValueAsString(Map.of(
                    "destination", dispatch.destination(),
                    "body", dispatch.body(),
                    "channel", dispatch.channel(),
                    "status", "queued"
            ));
            Files.writeString(outboxFile, line + System.lineSeparator(), StandardOpenOption.APPEND);
            return "queued:file:" + dispatch.channel() + ":" + dispatch.destination();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
