package ai.binbun.resources;

import java.util.List;

public record ResourceCatalog(List<DiscoveredResource> resources) {
    public ResourceCatalog {
        resources = List.copyOf(resources);
    }
}
