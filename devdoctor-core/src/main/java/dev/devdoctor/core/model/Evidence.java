package dev.devdoctor.core.model;

import java.time.Instant;
import java.util.Map;

public record Evidence(String id, EvidenceType type, EvidenceSource source, String summary,
                       EvidenceStrength strength, Sensitivity sensitivity, Instant collectedAt,
                       Map<String, Object> metadata) {
    public Evidence {
        id = ModelSupport.required(id, "id");
        if (type == null || source == null || strength == null || sensitivity == null) {
            throw new IllegalArgumentException("evidence type, source, strength and sensitivity are required");
        }
        summary = ModelSupport.required(summary, "summary");
        collectedAt = collectedAt == null ? Instant.now() : collectedAt;
        metadata = ModelSupport.map(metadata);
    }
}
