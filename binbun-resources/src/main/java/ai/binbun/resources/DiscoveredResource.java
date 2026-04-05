package ai.binbun.resources;

import java.nio.file.Path;

public record DiscoveredResource(ResourceType type, Path path, String scope) {}
