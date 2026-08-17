package dev.devdoctor.core.model;

import java.util.List;

public record RootCauseCandidate(String hypothesisId, String title, Confidence confidence,
                                 List<String> evidencePath, int rank) {
    public RootCauseCandidate {
        hypothesisId = ModelSupport.required(hypothesisId, "hypothesisId");
        title = ModelSupport.required(title, "title");
        if (confidence == null || rank < 1) throw new IllegalArgumentException("confidence and positive rank required");
        evidencePath = ModelSupport.list(evidencePath);
    }
}
