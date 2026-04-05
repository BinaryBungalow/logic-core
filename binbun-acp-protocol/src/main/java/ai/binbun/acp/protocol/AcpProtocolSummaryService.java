package ai.binbun.acp.protocol;

import java.util.Arrays;
import java.util.List;

public final class AcpProtocolSummaryService {
    public String version() {
        return AcpProtocolVersion.V1ALPHA1;
    }

    public List<AcpOperation> supportedOperations() {
        return Arrays.stream(AcpOperation.values()).toList();
    }
}
