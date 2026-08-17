package dev.devdoctor.core.spi;

import dev.devdoctor.core.model.Hypothesis;
import java.util.List;

public interface HypothesisReasoner {
    List<Hypothesis> propose(SanitizedDiagnosticContext context);
}
