package dev.devdoctor.core.spi;

import dev.devdoctor.core.model.ProjectProfile;
import java.util.List;

public interface DevDoctorPlugin {
    String id();
    boolean supports(ProjectProfile profile);
    default List<EvidenceCollector> collectors() { return List.of(); }
    default List<DiagnosticRule> rules() { return List.of(); }
    default List<DiagnosticProbe> probes() { return List.of(); }
}
