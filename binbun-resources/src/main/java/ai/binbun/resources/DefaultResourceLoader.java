package ai.binbun.resources;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class DefaultResourceLoader implements ResourceLoader {
    private final Path globalRoot;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultResourceLoader() {
        this(Path.of(System.getProperty("user.home"), ".pi-java"));
    }

    public DefaultResourceLoader(Path globalRoot) {
        this.globalRoot = globalRoot;
    }

    @Override
    public ResourceCatalog discover(Path projectRoot) {
        List<DiscoveredResource> resources = new ArrayList<>();
        scanScope(resources, globalRoot, "global");
        if (projectRoot != null) {
            scanScope(resources, projectRoot.resolve(".pi-java"), "project");
        }
        resources.sort(Comparator.comparing(DiscoveredResource::scope)
                .thenComparing(r -> r.type().name())
                .thenComparing(r -> r.path().toString()));
        return new ResourceCatalog(resources);
    }

    @Override
    public RuntimeResourceContext loadRuntimeContext(Path projectRoot, List<String> requestedPrompts,
                                                     List<String> requestedSkills, List<String> requestedExtensions) {
        ResourceCatalog catalog = discover(projectRoot);
        List<PromptManifest> prompts = new ArrayList<>();
        List<SkillManifest> skills = new ArrayList<>();
        List<ExtensionManifest> extensions = new ArrayList<>();
        Set<String> wantedPrompts = normalizeSet(requestedPrompts);
        Set<String> wantedSkills = normalizeSet(requestedSkills);
        Set<String> wantedExtensions = normalizeSet(requestedExtensions);

        for (DiscoveredResource resource : catalog.resources()) {
            switch (resource.type()) {
                case PROMPT -> {
                    PromptManifest prompt = readPrompt(resource.path());
                    if (shouldLoad(prompt.name(), prompt.autoload(), wantedPrompts)) {
                        prompts.add(prompt);
                    }
                }
                case SKILL -> {
                    SkillManifest skill = readSkill(resource.path());
                    if (shouldLoad(skill.name(), skill.autoload(), wantedSkills)) {
                        skills.add(skill);
                    }
                }
                case EXTENSION -> {
                    ExtensionManifest extension = readExtension(resource.path());
                    if (shouldLoad(extension.name(), extension.autoload(), wantedExtensions)) {
                        extensions.add(extension);
                    }
                }
                default -> {
                }
            }
        }
        return new RuntimeResourceContext(prompts, skills, extensions, catalog);
    }

    private boolean shouldLoad(String name, boolean autoload, Set<String> requested) {
        if (!requested.isEmpty()) {
            return requested.contains(name.toLowerCase(Locale.ROOT));
        }
        return autoload;
    }

    private Set<String> normalizeSet(List<String> values) {
        Set<String> normalized = new HashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private PromptManifest readPrompt(Path path) {
        if (path.getFileName().toString().endsWith(".json")) {
            try {
                return objectMapper.readValue(path.toFile(), PromptManifest.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        String base = stripExtension(path.getFileName().toString());
        return new PromptManifest(base, base, List.of(), true, readText(path));
    }

    private SkillManifest readSkill(Path path) {
        if (path.getFileName().toString().endsWith(".json")) {
            try {
                return objectMapper.readValue(path.toFile(), SkillManifest.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        String base = stripExtension(path.getFileName().toString());
        return new SkillManifest(base, base, List.of(), false, readText(path), List.of(), base);
    }

    private ExtensionManifest readExtension(Path path) {
        if (path.getFileName().toString().endsWith(".json")) {
            try {
                return objectMapper.readValue(path.toFile(), ExtensionManifest.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        String base = stripExtension(path.getFileName().toString());
        return new ExtensionManifest(base, base, false, List.of(readText(path)), List.of(), List.of(), base);
    }

    private String stripExtension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? name : name.substring(0, index);
    }

    private void scanScope(List<DiscoveredResource> resources, Path root, String scope) {
        scanDir(resources, root.resolve("prompts"), ResourceType.PROMPT, scope);
        scanDir(resources, root.resolve("skills"), ResourceType.SKILL, scope);
        scanDir(resources, root.resolve("extensions"), ResourceType.EXTENSION, scope);
        scanDir(resources, root.resolve("themes"), ResourceType.THEME, scope);
        scanDir(resources, root.resolve("models"), ResourceType.MODEL, scope);
        scanFile(resources, root.resolve("pi.json"), ResourceType.CONFIG, scope);
        scanFile(resources, root.resolve("pi.yaml"), ResourceType.CONFIG, scope);
        scanFile(resources, root.resolve("pi.yml"), ResourceType.CONFIG, scope);
    }

    private void scanDir(List<DiscoveredResource> resources, Path dir, ResourceType type, String scope) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> resources.add(new DiscoveredResource(type, path, scope)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void scanFile(List<DiscoveredResource> resources, Path file, ResourceType type, String scope) {
        if (Files.isRegularFile(file)) {
            resources.add(new DiscoveredResource(type, file, scope));
        }
    }

    private String readText(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
