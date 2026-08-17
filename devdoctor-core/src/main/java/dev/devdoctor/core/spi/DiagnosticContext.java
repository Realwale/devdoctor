package dev.devdoctor.core.spi;

import dev.devdoctor.core.model.Evidence;
import dev.devdoctor.core.model.Failure;
import dev.devdoctor.core.model.ProbeSafety;
import dev.devdoctor.core.model.ProjectProfile;
import java.nio.file.Path;
import java.util.List;

public record DiagnosticContext(Path projectRoot, Failure failure, ProjectProfile project,
                                List<Evidence> evidence, ProbeSafety maximumSafety) {
    public DiagnosticContext {
        projectRoot = projectRoot.toAbsolutePath().normalize();
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        maximumSafety = maximumSafety == null ? ProbeSafety.SAFE_ACTIVE : maximumSafety;
    }
}
