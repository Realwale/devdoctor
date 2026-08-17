package dev.devdoctor.core.spi;

import dev.devdoctor.core.model.Evidence;
import java.util.List;

public interface EvidenceCollector {
    String id();
    boolean supports(DiagnosticContext context);
    List<Evidence> collect(DiagnosticContext context);
}
