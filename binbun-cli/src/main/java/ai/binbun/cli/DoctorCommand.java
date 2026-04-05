package ai.binbun.cli;

import ai.binbun.model.ModelClients;
import ai.binbun.model.ProviderKind;
import picocli.CommandLine.Command;

@Command(name = "doctor", mixinStandardHelpOptions = true, description = "Print provider defaults and environment guidance")
public final class DoctorCommand implements Runnable {
    @Override
    public void run() {
        for (ProviderKind kind : ProviderKind.values()) {
            var profile = ModelClients.profile(kind);
            System.out.println(kind.name().toLowerCase() + " baseUrl=" + profile.defaultBaseUrl() +
                    " model=" + profile.defaultModel() +
                    " tools=" + profile.capabilities().supportsTools() +
                    " streaming=" + profile.capabilities().supportsStreaming());
        }
        System.out.println("env BINBUN_API_KEY, optional BINBUN_BASE_URL, optional BINBUN_MODEL");
    }
}
