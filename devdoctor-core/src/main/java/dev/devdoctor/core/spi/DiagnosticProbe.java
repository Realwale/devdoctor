package dev.devdoctor.core.spi;

import dev.devdoctor.core.model.ProbeResult;
import dev.devdoctor.core.model.ProbeSafety;

public interface DiagnosticProbe {
    String id();
    boolean supports(DiagnosticContext context);
    ProbeSafety safety();
    ProbeResult execute(DiagnosticContext context);
}
