package dev.devdoctor.core.model;

import java.time.Instant;
import java.util.List;

public record Failure(String id, String summary, String exceptionClass, String message,
                      List<FailureClassification> classifications, Instant observedAt) {
    public Failure {
        id = ModelSupport.required(id, "id");
        summary = ModelSupport.required(summary, "summary");
        exceptionClass = exceptionClass == null ? "" : exceptionClass;
        message = message == null ? "" : message;
        classifications = ModelSupport.list(classifications);
        observedAt = observedAt == null ? Instant.now() : observedAt;
    }
}
