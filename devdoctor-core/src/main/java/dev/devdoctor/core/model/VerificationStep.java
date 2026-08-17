package dev.devdoctor.core.model;

public record VerificationStep(String id, String hypothesisId, String description, String command, boolean safe) {
    public VerificationStep {
        id = ModelSupport.required(id, "id");
        hypothesisId = ModelSupport.required(hypothesisId, "hypothesisId");
        description = ModelSupport.required(description, "description");
        command = command == null ? "" : command;
    }
}
