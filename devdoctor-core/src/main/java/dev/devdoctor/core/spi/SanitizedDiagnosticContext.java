package dev.devdoctor.core.spi;

import dev.devdoctor.core.model.Evidence;
import dev.devdoctor.core.model.Failure;
import dev.devdoctor.core.model.ProjectProfile;
import java.util.List;

public record SanitizedDiagnosticContext(Failure failure, ProjectProfile project, List<Evidence> evidence) {
    public SanitizedDiagnosticContext { evidence = evidence == null ? List.of() : List.copyOf(evidence); }
}
