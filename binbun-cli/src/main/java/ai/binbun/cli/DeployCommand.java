package ai.binbun.cli;

import ai.binbun.deploy.DeploymentSpec;
import ai.binbun.deploy.SshDeploymentProvider;
import ai.binbun.deploy.SshTarget;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "deploy", mixinStandardHelpOptions = true, subcommands = {
        DeployCommand.PlanCommand.class,
        DeployCommand.ApplySshCommand.class,
        DeployCommand.ListCommand.class,
        DeployCommand.HealthSshCommand.class,
        DeployCommand.LogsSshCommand.class,
        DeployCommand.StopSshCommand.class
})
public final class DeployCommand implements Runnable {
    static final SshDeploymentProvider PROVIDER = new SshDeploymentProvider();

    @Override
    public void run() {
    }

    @Command(name = "plan", description = "Create a deployment plan")
    public static final class PlanCommand implements Runnable {
        @Option(names = "--provider", defaultValue = "ssh-vllm") String provider;
        @Option(names = "--model", required = true) String model;
        @Option(names = "--gpus", defaultValue = "1") int gpus;
        @Option(names = "--accelerator", defaultValue = "L40S") String accelerator;
        @Option(names = "--port", defaultValue = "8000") int port;

        @Override
        public void run() {
            var handle = PROVIDER.plan(new DeploymentSpec(provider, model, gpus, accelerator, port));
            System.out.println("planned " + handle.id() + " -> " + handle.endpoint());
        }
    }

    @Command(name = "apply-ssh", description = "Provision and launch vLLM over SSH")
    public static final class ApplySshCommand implements Runnable {
        @Option(names = "--model", required = true) String model;
        @Option(names = "--gpus", defaultValue = "1") int gpus;
        @Option(names = "--accelerator", defaultValue = "L40S") String accelerator;
        @Option(names = "--port", defaultValue = "8000") int port;
        @Option(names = "--host", required = true) String host;
        @Option(names = "--user", required = true) String user;
        @Option(names = "--ssh-port", defaultValue = "22") int sshPort;
        @Option(names = "--workdir", defaultValue = "~/pi-java-vllm") String workdir;

        @Override
        public void run() {
            var handle = PROVIDER.apply(new DeploymentSpec("ssh-vllm", model, gpus, accelerator, port),
                    new SshTarget(host, user, sshPort, workdir));
            System.out.println("running " + handle.id() + " -> " + handle.endpoint());
        }
    }

    @Command(name = "list", description = "List deployments")
    public static final class ListCommand implements Runnable {
        @Override
        public void run() {
            for (var handle : PROVIDER.list()) {
                System.out.println(handle.id() + " " + handle.provider() + " " + handle.status() + " " + handle.endpoint());
            }
        }
    }

    @Command(name = "health-ssh", description = "Probe remote health endpoint")
    public static final class HealthSshCommand implements Runnable {
        @Option(names = "--id", required = true) String id;
        @Option(names = "--host", required = true) String host;
        @Option(names = "--user", required = true) String user;
        @Option(names = "--ssh-port", defaultValue = "22") int sshPort;
        @Option(names = "--workdir", defaultValue = "~/pi-java-vllm") String workdir;

        @Override
        public void run() {
            boolean healthy = PROVIDER.health(id, new SshTarget(host, user, sshPort, workdir));
            System.out.println(id + " healthy=" + healthy);
        }
    }

    @Command(name = "logs-ssh", description = "Read remote vLLM logs over SSH")
    public static final class LogsSshCommand implements Runnable {
        @Option(names = "--id", required = true) String id;
        @Option(names = "--host", required = true) String host;
        @Option(names = "--user", required = true) String user;
        @Option(names = "--ssh-port", defaultValue = "22") int sshPort;
        @Option(names = "--workdir", defaultValue = "~/pi-java-vllm") String workdir;

        @Override
        public void run() {
            String logs = PROVIDER.logs(id, new SshTarget(host, user, sshPort, workdir));
            System.out.println(logs);
        }
    }

    @Command(name = "stop-ssh", description = "Stop a remote vLLM process over SSH")
    public static final class StopSshCommand implements Runnable {
        @Option(names = "--id", required = true) String id;
        @Option(names = "--host", required = true) String host;
        @Option(names = "--user", required = true) String user;
        @Option(names = "--ssh-port", defaultValue = "22") int sshPort;
        @Option(names = "--workdir", defaultValue = "~/pi-java-vllm") String workdir;

        @Override
        public void run() {
            var handle = PROVIDER.stop(id, new SshTarget(host, user, sshPort, workdir));
            System.out.println(handle.id() + " " + handle.status());
        }
    }
}
