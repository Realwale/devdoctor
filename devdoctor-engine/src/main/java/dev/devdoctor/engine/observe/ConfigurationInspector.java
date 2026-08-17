package dev.devdoctor.engine.observe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.engine.security.SecretRedactor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigurationInspector {
    private static final long MAX_FILE = 1_000_000;
    private static final List<String> FILES = List.of("application.yml", "application.yaml", "application.properties", "bootstrap.yml", "bootstrap.yaml", ".env", "Dockerfile", "compose.yml", "compose.yaml", "docker-compose.yml", "docker-compose.yaml", "pom.xml", "build.gradle", "build.gradle.kts");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)(?::[^}]*)?}");
    private final SecretRedactor redactor;
    public ConfigurationInspector(SecretRedactor redactor) { this.redactor = redactor; }

    public Inspection inspect(Path root, Map<String, String> environment) {
        List<Evidence> evidence = new ArrayList<>(); Set<String> variables = new LinkedHashSet<>(); int id = 1;
        for (String relative : FILES) {
            Path file = root.resolve(relative).normalize();
            if (!file.startsWith(root.toAbsolutePath().normalize()) || !Files.isRegularFile(file)) continue;
            try {
                if (Files.size(file) > MAX_FILE) continue;
                String raw = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = PLACEHOLDER.matcher(raw); while (matcher.find()) variables.add(matcher.group(1));
                Map<String,Object> metadata = Map.of("path", relative, "size", Files.size(file), "environmentReferences", variables.stream().filter(raw::contains).sorted().toList());
                evidence.add(new Evidence("E-CFG-" + id++, EvidenceType.CONFIGURATION,
                        new EvidenceSource("FILE", relative, Map.of()), "Configuration file inspected: " + relative,
                        EvidenceStrength.MEDIUM, Sensitivity.INTERNAL, Instant.now(), metadata));
            } catch (IOException ignored) { }
        }
        for (String variable : variables) {
            Map<String,Object> shape = redactor.characteristics(variable, environment.get(variable));
            String summary = variable + " is " + ((boolean) shape.get("present") ? "present" : "missing")
                    + ((boolean) shape.get("trailingNewline") ? " and contains a trailing newline" : "");
            evidence.add(new Evidence("E-ENV-" + id++, EvidenceType.ENVIRONMENT,
                    new EvidenceSource("ENVIRONMENT", variable, Map.of()), summary,
                    (boolean) shape.get("trailingNewline") ? EvidenceStrength.VERY_HIGH : EvidenceStrength.HIGH,
                    Sensitivity.SECRET_REDACTED, Instant.now(), shape));
        }
        return new Inspection(evidence, List.copyOf(variables));
    }

    public record Inspection(List<Evidence> evidence, List<String> referencedEnvironmentVariables) {}
}
