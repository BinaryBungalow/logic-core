package ai.binbun.resources;

import java.nio.file.Path;
import java.util.List;

public interface ResourceLoader {
    ResourceCatalog discover(Path projectRoot);
    RuntimeResourceContext loadRuntimeContext(Path projectRoot, List<String> requestedPrompts,
                                              List<String> requestedSkills, List<String> requestedExtensions);
}
