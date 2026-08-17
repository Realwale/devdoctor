package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import dev.devdoctor.engine.observe.BoundedCommandRunner;
import dev.devdoctor.engine.security.SecretRedactor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class PortAvailabilityProbe implements DiagnosticProbe {
    private final SecretRedactor redactor;
    public PortAvailabilityProbe(SecretRedactor redactor) { this.redactor = redactor; }
    public String id() { return "port-availability"; }
    public boolean supports(DiagnosticContext c) { return ProbeSupport.port(c).isPresent(); }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        int port = ProbeSupport.port(c).orElseThrow(); Instant start = Instant.now();
        var result = new BoundedCommandRunner(redactor, Duration.ofSeconds(2), 10_000).run(c.projectRoot(), "lsof -nP -iTCP:" + port + " -sTCP:LISTEN"); boolean available = result.exitCode() != 0 || result.stdout().isBlank();
        Evidence evidence = new Evidence("E-PROBE-PORT-AVAIL", EvidenceType.PORT, new EvidenceSource("PROCESS_TABLE", "tcp:" + port, Map.of()), available ? "TCP port appears available" : "TCP port is occupied", EvidenceStrength.HIGH, Sensitivity.INTERNAL, Instant.now(), Map.of("port", port, "available", available));
        return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED, evidence.summary(), start, Duration.between(start, Instant.now()), List.of(evidence), evidence.metadata());
    }
}
