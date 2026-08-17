package dev.devdoctor.core.spi;

import dev.devdoctor.core.model.Hypothesis;
import java.util.List;

public interface DiagnosticRule {
    String id();
    boolean supports(DiagnosticContext context);
    List<Hypothesis> generateHypotheses(DiagnosticContext context);
}
