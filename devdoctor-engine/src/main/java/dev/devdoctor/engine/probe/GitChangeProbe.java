package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class GitChangeProbe implements DiagnosticProbe {
    public String id() { return "git-change"; }
    public boolean supports(DiagnosticContext c) { return c.evidence().stream().anyMatch(e -> e.type() == EvidenceType.GIT); }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        List<String> ids = c.evidence().stream().filter(e -> e.type() == EvidenceType.GIT).map(Evidence::id).toList();
        return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED, "Git changes are available as correlation-only evidence", Instant.now(), Duration.ZERO, List.of(), Map.of("evidence", ids, "causal", false));
    }
}
