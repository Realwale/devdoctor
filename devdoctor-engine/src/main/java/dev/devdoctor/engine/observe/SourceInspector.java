package dev.devdoctor.engine.observe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.engine.security.SecretRedactor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SourceInspector {
    private final SecretRedactor redactor;
    public SourceInspector(SecretRedactor redactor) { this.redactor = redactor; }

    public List<Evidence> inspect(Path root, List<StackFrame> frames) {
        List<Evidence> evidence = new ArrayList<>(); int id = 1;
        for (StackFrame frame : frames.stream().limit(3).toList()) {
            Optional<Path> path = findSource(root, frame.file());
            if (path.isEmpty() || frame.line() == null) continue;
            try {
                List<String> lines = Files.readAllLines(path.get(), StandardCharsets.UTF_8);
                int from = Math.max(1, frame.line() - 10); int to = Math.min(lines.size(), frame.line() + 10);
                String snippet = String.join("\n", lines.subList(from - 1, to));
                String relative = root.toAbsolutePath().normalize().relativize(path.get()).toString();
                evidence.add(new Evidence("E-SRC-" + id++, EvidenceType.SOURCE,
                        new EvidenceSource("SOURCE_FILE", relative + ":" + frame.line(), Map.of("method", frame.method())),
                        "Application boundary at " + relative + ":" + frame.line(), EvidenceStrength.HIGH,
                        Sensitivity.INTERNAL, Instant.now(), Map.of("path", relative, "line", frame.line(), "from", from, "to", to,
                        "snippet", redactor.redact(snippet), "reason", "application-owned stack frame")));
            } catch (IOException ignored) { }
        }
        return List.copyOf(evidence);
    }

    private Optional<Path> findSource(Path root, String fileName) {
        if (fileName == null || fileName.equals("Unknown Source") || fileName.equals("Native Method")) return Optional.empty();
        try (var stream = Files.find(root, 12, (p, a) -> a.isRegularFile() && p.getFileName().toString().equals(fileName)
                && !p.toString().contains("/target/") && !p.toString().contains("/build/") && !p.toString().contains("/.git/"))) {
            return stream.findFirst().map(Path::toAbsolutePath).map(Path::normalize).filter(p -> p.startsWith(root.toAbsolutePath().normalize()));
        } catch (IOException ignored) { return Optional.empty(); }
    }
}
