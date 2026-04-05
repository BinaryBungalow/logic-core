package ai.binbun.cli;

import picocli.CommandLine.Command;

@Command(name = "pi", mixinStandardHelpOptions = true, subcommands = {
        RunCommand.class,
        DeployCommand.class,
        DoctorCommand.class,
        GatewayCommand.class
})
public final class PiCommand implements Runnable {
    @Override
    public void run() {
    }
}
