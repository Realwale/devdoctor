package dev.devdoctor.core.json;

import static org.assertj.core.api.Assertions.assertThat;
import dev.devdoctor.core.model.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiagnosticJsonTest {
    @Test void roundTripsVersionedSession() {
        Instant now = Instant.parse("2026-08-16T10:00:00Z");
        var failure = new Failure("F-1", "test failed", "", "", List.of(FailureClassification.TEST), now);
        var project = new ProjectProfile("/tmp/app", "JAVA", "21", "MAVEN", "SPRING_BOOT", "3.3.8", "MVC", List.of(), Map.of());
        var graph = new DiagnosticGraph(List.of(new GraphNode("F-1", GraphNodeType.FAILURE, "test failed")), List.of());
        var session = new DiagnosticSession("1.0", "DD-TEST", now, failure, project, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), graph);
        var json = new DiagnosticJson().write(session);
        assertThat(json).contains("\"schemaVersion\" : \"1.0\"");
        assertThat(new DiagnosticJson().read(json)).isEqualTo(session);
    }
}
