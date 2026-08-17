package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.DiagnosticContext;
import dev.devdoctor.core.spi.DiagnosticProbe;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class JavaVersionProbe implements DiagnosticProbe {
    public String id() { return "java-version"; }
    public boolean supports(DiagnosticContext context) { return "JAVA".equals(context.project().language()); }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext context) {
        Instant now = Instant.now(); String version = System.getProperty("java.version", "unknown");
        Evidence evidence = new Evidence("E-PROBE-JAVA", EvidenceType.JVM, new EvidenceSource("JVM", "java.version", Map.of()),
                "Diagnostic runtime Java version is " + version, EvidenceStrength.HIGH, Sensitivity.PUBLIC, now, Map.of("version", version));
        return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED, evidence.summary(), now, Duration.ZERO, List.of(evidence), Map.of("version", version));
    }
}
