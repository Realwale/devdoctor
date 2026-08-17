package dev.devdoctor.core.model;

public record Remediation(String id, String hypothesisId, String description, boolean automatic) {
    public Remediation {
        id = ModelSupport.required(id, "id");
        hypothesisId = ModelSupport.required(hypothesisId, "hypothesisId");
        description = ModelSupport.required(description, "description");
        if (automatic) throw new IllegalArgumentException("V1 remediation cannot be automatic");
    }
}
