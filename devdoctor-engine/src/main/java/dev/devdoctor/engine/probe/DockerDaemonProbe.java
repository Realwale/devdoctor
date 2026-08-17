package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import dev.devdoctor.engine.observe.BoundedCommandRunner;
import dev.devdoctor.engine.security.SecretRedactor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DockerDaemonProbe implements DiagnosticProbe {
    private final SecretRedactor redactor;
    public DockerDaemonProbe(SecretRedactor redactor) { this.redactor = redactor; }
    public String id() { return "docker-daemon"; }
    public boolean supports(DiagnosticContext c) { return c.project().technologies().stream().anyMatch(t -> t.startsWith("DOCKER")); }
    public ProbeSafety safety() { return ProbeSafety.SAFE_ACTIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        Instant start = Instant.now(); var capture = new BoundedCommandRunner(redactor, Duration.ofSeconds(3), 10_000).run(c.projectRoot(), "docker info --format '{{.ServerVersion}}'"); boolean available = capture.exitCode() == 0;
        Evidence e = new Evidence("E-PROBE-DOCKER", EvidenceType.DOCKER, new EvidenceSource("DOCKER", "daemon", Map.of()), available ? "Docker daemon responded" : "Docker daemon did not respond", EvidenceStrength.HIGH, Sensitivity.INTERNAL, Instant.now(), Map.of("available", available, "serverVersion", capture.stdout().trim()));
        return new ProbeResult(id(), safety(), available ? ProbeStatus.SUCCEEDED : ProbeStatus.FAILED, e.summary(), start, Duration.between(start, Instant.now()), List.of(e), e.metadata());
    }
}
