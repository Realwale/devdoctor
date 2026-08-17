package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DependencyVersionProbe implements DiagnosticProbe {
    private static final Pattern VERSION = Pattern.compile("(?s)<dependency>.*?<groupId>([^<]+)</groupId>.*?<artifactId>([^<]+)</artifactId>.*?(?:<version>([^<]+)</version>)?.*?</dependency>");
    public String id() { return "dependency-version"; }
    public boolean supports(DiagnosticContext c) { return Files.isRegularFile(c.projectRoot().resolve("pom.xml")) || Files.isRegularFile(c.projectRoot().resolve("build.gradle")) || Files.isRegularFile(c.projectRoot().resolve("build.gradle.kts")); }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        Path path = Files.isRegularFile(c.projectRoot().resolve("pom.xml")) ? c.projectRoot().resolve("pom.xml") : Files.isRegularFile(c.projectRoot().resolve("build.gradle")) ? c.projectRoot().resolve("build.gradle") : c.projectRoot().resolve("build.gradle.kts");
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8); Matcher matcher = VERSION.matcher(text); int explicit = 0; java.util.ArrayList<String> declarations = new java.util.ArrayList<>();
            while (matcher.find()) { String version = matcher.group(3); if (version != null) explicit++; declarations.add(matcher.group(1).trim() + ":" + matcher.group(2).trim() + ":" + (version == null ? "BOM/managed" : version.trim())); }
            String summary = explicit > 0 ? "Build declares " + explicit + " explicit dependency version(s), which can override a framework BOM" : "Dependencies use managed or implicit versions";
            Evidence e = new Evidence("E-PROBE-DEPENDENCY", EvidenceType.DEPENDENCY, new EvidenceSource("BUILD_FILE", path.getFileName().toString(), Map.of()), summary, EvidenceStrength.HIGH, Sensitivity.INTERNAL, Instant.now(), Map.of("explicitVersionDeclarations", explicit, "declarations", declarations, "buildFile", path.getFileName().toString(), "limitation", "declared versions only; runtime tree requires build-tool resolution"));
            return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED, e.summary(), Instant.now(), Duration.ZERO, List.of(e), e.metadata());
        } catch (IOException e) { return new ProbeResult(id(), safety(), ProbeStatus.FAILED, "Dependency declarations could not be read", Instant.now(), Duration.ZERO, List.of(), Map.of()); }
    }
}
