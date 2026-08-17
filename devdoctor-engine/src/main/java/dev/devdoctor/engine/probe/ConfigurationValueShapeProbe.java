package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ConfigurationValueShapeProbe implements DiagnosticProbe {
    public String id() { return "configuration-value-shape"; }
    public boolean supports(DiagnosticContext c) { return c.evidence().stream().anyMatch(e -> e.type() == EvidenceType.ENVIRONMENT || e.type() == EvidenceType.CONFIGURATION); }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        List<String> anomalous = c.evidence().stream().filter(e -> Boolean.TRUE.equals(e.metadata().get("blank")) || Boolean.TRUE.equals(e.metadata().get("trailingWhitespace"))).map(Evidence::id).toList();
        return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED, anomalous.isEmpty() ? "No inspected configuration shape anomaly" : "Configuration shape anomalies observed", Instant.now(), Duration.ZERO, List.of(), Map.of("evidence", anomalous));
    }
}
