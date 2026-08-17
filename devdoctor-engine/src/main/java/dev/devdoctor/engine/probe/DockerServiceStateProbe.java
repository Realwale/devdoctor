package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import dev.devdoctor.engine.observe.BoundedCommandRunner;
import dev.devdoctor.engine.security.SecretRedactor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DockerServiceStateProbe implements DiagnosticProbe {
    private final SecretRedactor redactor;
    public DockerServiceStateProbe(SecretRedactor redactor) { this.redactor = redactor; }
    public String id() { return "docker-service-state"; }
    public boolean supports(DiagnosticContext c) { return c.project().technologies().contains("DOCKER_COMPOSE"); }
    public ProbeSafety safety() { return ProbeSafety.SAFE_ACTIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        Instant start = Instant.now(); var capture = new BoundedCommandRunner(redactor, Duration.ofSeconds(3), 50_000).run(c.projectRoot(), "docker compose ps --format json"); boolean read = capture.exitCode() == 0;
        Evidence e = new Evidence("E-PROBE-COMPOSE", EvidenceType.DOCKER, new EvidenceSource("DOCKER_COMPOSE", "services", Map.of()), read ? "Docker Compose service state collected" : "Docker Compose service state unavailable", EvidenceStrength.HIGH, Sensitivity.INTERNAL, Instant.now(), Map.of("state", capture.stdout(), "read", read));
        return new ProbeResult(id(), safety(), read ? ProbeStatus.SUCCEEDED : ProbeStatus.FAILED, e.summary(), start, Duration.between(start, Instant.now()), List.of(e), e.metadata());
    }
}
