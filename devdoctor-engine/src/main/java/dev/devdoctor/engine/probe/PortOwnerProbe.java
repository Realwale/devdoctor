package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.DiagnosticContext;
import dev.devdoctor.core.spi.DiagnosticProbe;
import dev.devdoctor.engine.observe.BoundedCommandRunner;
import dev.devdoctor.engine.security.SecretRedactor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class PortOwnerProbe implements DiagnosticProbe {
    private final SecretRedactor redactor;
    public PortOwnerProbe(SecretRedactor redactor) { this.redactor = redactor; }
    public String id() { return "port-owner"; }
    public boolean supports(DiagnosticContext context) { return ProbeSupport.port(context).isPresent(); }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext context) {
        int port = ProbeSupport.port(context).orElseThrow(); Instant start = Instant.now();
        var capture = new BoundedCommandRunner(redactor, Duration.ofSeconds(2), 20_000).run(context.projectRoot(), "lsof -nP -iTCP:" + port + " -sTCP:LISTEN");
        boolean occupied = capture.exitCode() == 0 && !capture.stdout().isBlank();
        Evidence e = new Evidence("E-PROBE-PORT", EvidenceType.PORT, new EvidenceSource("PROCESS_TABLE", "tcp:" + port, Map.of()),
                occupied ? "A process owns TCP port " + port : "No listening process was observed on TCP port " + port,
                EvidenceStrength.HIGH, Sensitivity.INTERNAL, Instant.now(), Map.of("port", port, "occupied", occupied, "processTable", capture.stdout()));
        return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED, e.summary(), start, Duration.between(start, Instant.now()), List.of(e), Map.of("port", port, "occupied", occupied));
    }
}
