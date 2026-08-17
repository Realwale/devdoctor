package dev.devdoctor.engine;

import static org.assertj.core.api.Assertions.assertThat;
import dev.devdoctor.core.json.DiagnosticJson;
import dev.devdoctor.core.model.ProbeSafety;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiagnosticEngineTest {
    @Test void provesInvalidHeaderTrailingNewlineWithoutLeakingSecret(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>fixture</artifactId><version>1</version><properties><java.version>21</java.version></properties></project>");
        Files.writeString(root.resolve("application.properties"), "fonu.api-key=${FONU_API_KEY}\n");
        String secret = "SuperSecretKey0123456789\n";
        String log = "java.lang.IllegalArgumentException: Validation failed for header 'x-fonu-api-key'\n\tat com.acme.FonuClientConfig.configure(FonuClientConfig.java:47)";
        var request = new DiagnosticRequest(root, "", log, Map.of("FONU_API_KEY", secret), true, ProbeSafety.PASSIVE, Duration.ofSeconds(2), 100_000);
        var session = new DiagnosticEngine().diagnose(request);
        assertThat(session.rootCauses()).isNotEmpty();
        var rootHypothesis = session.hypotheses().stream().filter(h -> h.id().equals(session.rootCauses().getFirst().hypothesisId())).findFirst().orElseThrow();
        assertThat(rootHypothesis.ruleId()).isEqualTo("HTTP-01");
        assertThat(new DiagnosticJson().write(session)).doesNotContain(secret.trim()).contains("SECRET_REDACTED", "trailingNewline");
        assertThat(session.graph().edges()).anyMatch(e -> e.relationship().name().equals("SUPPORTS"));
    }

    @Test void doesNotManufactureFailureForHealthyProject(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>healthy</artifactId><version>1</version></project>");
        var session = new DiagnosticEngine().diagnose(DiagnosticRequest.passive(root, "2026-08-16T10:00:00Z INFO [main] Application started"));
        assertThat(session.failure().summary()).isEqualTo("NO FAILURE DETECTED"); assertThat(session.rootCauses()).isEmpty(); assertThat(session.hypotheses()).isEmpty();
    }
}
