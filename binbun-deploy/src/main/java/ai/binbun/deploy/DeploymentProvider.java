package ai.binbun.deploy;

import java.util.List;

public interface DeploymentProvider {
    String name();
    DeploymentHandle plan(DeploymentSpec spec);
    DeploymentHandle apply(DeploymentSpec spec, SshTarget target);
    List<DeploymentHandle> list();
    DeploymentHandle stop(String id, SshTarget target);
    String logs(String id, SshTarget target);
    boolean health(String id, SshTarget target);
}
