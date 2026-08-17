package dev.devdoctor.engine.knowledge;

import dev.devdoctor.core.model.FailureClassification;
import java.util.List;

public record DiagnosticSignature(String id, String title, String description,
                                  List<String> requiredPatterns, List<FailureClassification> classifications,
                                  List<String> probeIds, String remediation, String verification,
                                  boolean causalLinkage) {
    public DiagnosticSignature {
        requiredPatterns = List.copyOf(requiredPatterns);
        classifications = List.copyOf(classifications);
        probeIds = List.copyOf(probeIds);
    }
}
