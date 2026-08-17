package dev.devdoctor.engine.observe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.engine.security.SecretRedactor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GitInspector {
    private final SecretRedactor redactor;
    public GitInspector(SecretRedactor redactor) { this.redactor = redactor; }
    public List<Evidence> inspect(Path root) {
        if (!Files.isDirectory(root.resolve(".git"))) return List.of();
        var runner = new BoundedCommandRunner(redactor, Duration.ofSeconds(2), 100_000);
        List<Evidence> evidence = new ArrayList<>(); int id = 1;
        for (String command : List.of("git status --short", "git diff --stat", "git log -5 --format=%h%x09%aI%x09%s")) {
            CommandCapture capture = runner.run(root, command);
            evidence.add(new Evidence("E-GIT-" + id++, EvidenceType.GIT, new EvidenceSource("GIT", command, Map.of()),
                    capture.stdout().isBlank() ? "No Git observations for " + command : "Git observations collected for " + command,
                    EvidenceStrength.LOW, Sensitivity.INTERNAL, Instant.now(), Map.of("command", command, "exitCode", capture.exitCode(), "output", capture.stdout(), "correlationOnly", true)));
        }
        return List.copyOf(evidence);
    }
}
