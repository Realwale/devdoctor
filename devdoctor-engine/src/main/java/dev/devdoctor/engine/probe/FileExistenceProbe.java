package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class FileExistenceProbe implements DiagnosticProbe {
    public String id() { return "file-existence"; }
    public boolean supports(DiagnosticContext c) { return true; }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        boolean exists = Files.isDirectory(c.projectRoot());
        return new ProbeResult(id(), safety(), exists ? ProbeStatus.SUCCEEDED : ProbeStatus.FAILED, exists ? "Project root exists" : "Project root is unavailable", Instant.now(), Duration.ZERO, List.of(), Map.of("exists", exists));
    }
}
