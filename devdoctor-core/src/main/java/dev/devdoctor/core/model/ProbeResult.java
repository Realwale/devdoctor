package dev.devdoctor.core.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProbeResult(String probeId, ProbeSafety safety, ProbeStatus status, String summary,
                          Instant executedAt, Duration duration, List<Evidence> evidence,
                          Map<String, Object> sanitizedDetails) {
    public ProbeResult {
        probeId = ModelSupport.required(probeId, "probeId");
        if (safety == null || status == null) throw new IllegalArgumentException("probe safety and status are required");
        summary = ModelSupport.required(summary, "summary");
        executedAt = executedAt == null ? Instant.now() : executedAt;
        duration = duration == null ? Duration.ZERO : duration;
        evidence = ModelSupport.list(evidence);
        sanitizedDetails = ModelSupport.map(sanitizedDetails);
    }
}
