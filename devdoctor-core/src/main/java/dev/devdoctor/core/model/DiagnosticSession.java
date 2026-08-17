package dev.devdoctor.core.model;

import java.time.Instant;
import java.util.List;

public record DiagnosticSession(String schemaVersion, String diagnosticId, Instant startedAt,
                                Failure failure, ProjectProfile project, List<Evidence> evidence,
                                List<Hypothesis> hypotheses, List<ProbeResult> probes,
                                List<RootCauseCandidate> rootCauses, List<Remediation> remediations,
                                List<VerificationStep> verification, DiagnosticGraph graph) {
    public DiagnosticSession {
        schemaVersion = ModelSupport.required(schemaVersion, "schemaVersion");
        diagnosticId = ModelSupport.required(diagnosticId, "diagnosticId");
        startedAt = startedAt == null ? Instant.now() : startedAt;
        if (failure == null || project == null || graph == null) throw new IllegalArgumentException("failure, project and graph are required");
        evidence = ModelSupport.list(evidence);
        hypotheses = ModelSupport.list(hypotheses);
        probes = ModelSupport.list(probes);
        rootCauses = ModelSupport.list(rootCauses);
        remediations = ModelSupport.list(remediations);
        verification = ModelSupport.list(verification);
    }
}
