package ai.binbun.model;

public final class RequestValidator {
    private RequestValidator() {
    }

    public static void validate(ChatRequest request, ProviderProfile profile) {
        if (!profile.capabilities().supportsSystemMessages()) {
            boolean hasSystem = request.messages().stream().anyMatch(m -> "system".equals(m.role()));
            if (hasSystem) {
                throw new ModelException(profile.kind(), "Provider does not support system messages in this adapter", null);
            }
        }
        if (!profile.capabilities().supportsTools() && !request.tools().isEmpty()) {
            throw new ModelException(profile.kind(), "Provider does not support tools in this adapter", null);
        }
    }
}
