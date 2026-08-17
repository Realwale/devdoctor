package dev.devdoctor.engine;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.devdoctor.core.json.DiagnosticJson;
import dev.devdoctor.core.model.ProbeSafety;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FixtureIntegrationTest {
    @Test void diagnosesBrokenFixturesAndDoesNotOverdiagnoseHealthyFixtures() throws Exception {
        Path current = Path.of(System.getProperty("user.dir"));
        Path fixtures = current.getFileName().toString().equals("devdoctor-engine") ? current.resolve("../test-fixtures").normalize() : current.resolve("test-fixtures");
        ObjectMapper mapper = new ObjectMapper(); int scenarios = 0;
        try (var manifests = Files.find(fixtures, 3, (p, a) -> a.isRegularFile() && p.getFileName().toString().equals("fixture.json"))) {
            for (Path manifest : manifests.sorted().toList()) {
                scenarios++; JsonNode spec = mapper.readTree(manifest.toFile()); Path root = manifest.getParent();
                String log = Files.readString(root.resolve(spec.path("log").asText("failure.log"))); Map<String,String> environment = new HashMap<>();
                spec.path("environment").fields().forEachRemaining(e -> environment.put(e.getKey(), e.getValue().asText()));
                var session = new DiagnosticEngine().diagnose(new DiagnosticRequest(root, "", log, environment, true, ProbeSafety.PASSIVE, Duration.ofSeconds(2), 500_000));
                if (spec.path("healthy").asBoolean(false)) {
                    assertThat(session.rootCauses()).as(spec.path("name").asText()).isEmpty();
                } else {
                    assertThat(session.rootCauses()).as(spec.path("name").asText()).isNotEmpty();
                    String rule = session.hypotheses().stream().filter(h -> h.id().equals(session.rootCauses().getFirst().hypothesisId())).findFirst().orElseThrow().ruleId();
                    assertThat(rule).as(spec.path("name").asText()).isEqualTo(spec.path("expectedRuleId").asText());
                }
                String json = new DiagnosticJson().write(session);
                for (JsonNode canary : spec.path("secretCanaries")) assertThat(json).doesNotContain(canary.asText());
            }
        }
        assertThat(scenarios).isGreaterThanOrEqualTo(18);
    }
}
