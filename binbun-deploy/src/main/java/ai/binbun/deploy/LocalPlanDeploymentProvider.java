package ai.binbun.deploy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LocalPlanDeploymentProvider implements DeploymentProvider {
    private final List<DeploymentHandle> planned = new ArrayList<>();

    @Override
    public String name() {
        return "local-plan";
    }

    @Override
    public DeploymentHandle plan(DeploymentSpec spec) {
        DeploymentHandle handle = new DeploymentHandle(
                UUID.randomUUID().toString(),
                spec.provider(),
                "http://localhost:" + spec.port() + "/v1",
                "PLANNED"
        );
        planned.add(handle);
        return handle;
    }

    @Override
    public DeploymentHandle apply(DeploymentSpec spec, SshTarget target) {
        throw new UnsupportedOperationException("local-plan does not support apply");
    }

    @Override
    public List<DeploymentHandle> list() {
        return List.copyOf(planned);
    }

    @Override
    public DeploymentHandle stop(String id, SshTarget target) {
        DeploymentHandle handle = planned.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElse(new DeploymentHandle(id, name(), "", "NOT_FOUND"));
        if ("NOT_FOUND".equals(handle.status())) {
            return handle;
        }
        DeploymentHandle stopped = new DeploymentHandle(handle.id(), handle.provider(), handle.endpoint(), "STOPPED");
        planned.removeIf(p -> p.id().equals(id));
        planned.add(stopped);
        return stopped;
    }

    @Override
    public String logs(String id, SshTarget target) {
        return "";
    }

    @Override
    public boolean health(String id, SshTarget target) {
        return false;
    }
}
