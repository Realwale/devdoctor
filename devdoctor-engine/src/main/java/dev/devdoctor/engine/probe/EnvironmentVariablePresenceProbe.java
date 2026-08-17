package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.DiagnosticContext;
import dev.devdoctor.core.spi.DiagnosticProbe;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class EnvironmentVariablePresenceProbe implements DiagnosticProbe {
    public String id() { return "environment-presence"; }
    public boolean supports(DiagnosticContext context) { return context.evidence().stream().anyMatch(e -> e.type() == EvidenceType.ENVIRONMENT); }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext context) {
        List<String> missing = context.evidence().stream().filter(e -> e.type() == EvidenceType.ENVIRONMENT)
                .filter(e -> Boolean.FALSE.equals(e.metadata().get("present"))).map(e -> e.source().locator()).toList();
        List<String> presentEvidence = context.evidence().stream().filter(e -> e.type() == EvidenceType.ENVIRONMENT)
                .filter(e -> Boolean.TRUE.equals(e.metadata().get("present"))).map(Evidence::id).toList();
        return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED,
                missing.isEmpty() ? "All referenced environment variables are present" : "Referenced environment variables are missing",
                Instant.now(), Duration.ZERO, List.of(), Map.of("missing", missing, "presentEvidence", presentEvidence));
    }
}
