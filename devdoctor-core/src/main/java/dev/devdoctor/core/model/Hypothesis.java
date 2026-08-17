package dev.devdoctor.core.model;

import java.util.List;

public record Hypothesis(String id, String ruleId, String title, String description,
                         HypothesisStatus status, Confidence confidence,
                         List<String> supportingEvidence, List<String> contradictingEvidence,
                         List<String> availableProbes, List<String> confidenceFactors) {
    public Hypothesis {
        id = ModelSupport.required(id, "id");
        ruleId = ModelSupport.required(ruleId, "ruleId");
        title = ModelSupport.required(title, "title");
        description = description == null ? "" : description;
        status = status == null ? HypothesisStatus.UNTESTED : status;
        confidence = confidence == null ? Confidence.VERY_LOW : confidence;
        supportingEvidence = ModelSupport.list(supportingEvidence);
        contradictingEvidence = ModelSupport.list(contradictingEvidence);
        availableProbes = ModelSupport.list(availableProbes);
        confidenceFactors = ModelSupport.list(confidenceFactors);
    }

    public Hypothesis assessed(HypothesisStatus newStatus, Confidence newConfidence,
                               List<String> supporting, List<String> contradicting, List<String> factors) {
        return new Hypothesis(id, ruleId, title, description, newStatus, newConfidence,
                supporting, contradicting, availableProbes, factors);
    }
}
