package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.DiagnosticContext;
import dev.devdoctor.core.spi.DiagnosticProbe;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class EnvironmentWhitespaceProbe implements DiagnosticProbe {
    public String id() { return "environment-whitespace"; }
    public boolean supports(DiagnosticContext context) { return context.evidence().stream().anyMatch(e -> e.type() == EvidenceType.ENVIRONMENT); }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext context) {
        Instant now = Instant.now();
        List<Evidence> matching = context.evidence().stream().filter(e -> e.type() == EvidenceType.ENVIRONMENT)
                .filter(e -> Boolean.TRUE.equals(e.metadata().get("trailingWhitespace"))).toList();
        String summary = matching.isEmpty() ? "No referenced environment value has leading or trailing whitespace" : "Referenced environment value shape confirms trailing whitespace";
        return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED, summary, now, Duration.ZERO, List.of(), Map.of("matchingEvidence", matching.stream().map(Evidence::id).toList()));
    }
}
