package dev.devdoctor.engine;

import static org.assertj.core.api.Assertions.assertThat;
import dev.devdoctor.core.json.DiagnosticJson;
import dev.devdoctor.core.model.ProbeSafety;
import dev.devdoctor.engine.observe.JfrRuntimeObservation;
import dev.devdoctor.engine.observe.RuntimeExceptionGroup;
import dev.devdoctor.engine.observe.RuntimeTransactionGroup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
        assertThat(session.failure().summary()).isEqualTo("NO FAILURE REPRODUCED"); assertThat(session.rootCauses()).isEmpty(); assertThat(session.hypotheses()).isEmpty();
    }

    @Test void successfulBuildOutputWithZeroFailureCountersIsHealthy(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>healthy</artifactId><version>1</version></project>");
        String command = "printf 'Tests run: 4, Failures: 0, Errors: 0\\nBUILD SUCCESS\\n'";

        var session = new DiagnosticEngine().diagnose(new DiagnosticRequest(root, command, "", Map.of(), true,
                ProbeSafety.PASSIVE, Duration.ofSeconds(2), 100_000));

        assertThat(session.failure().summary()).isEqualTo("NO FAILURE REPRODUCED");
        assertThat(session.rootCauses()).isEmpty();
    }

    @Test void unrelatedMissingEnvironmentVariableCannotReplaceObservedJvmFailure(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>broken</artifactId><version>1</version></project>");
        Files.writeString(root.resolve("application.properties"), "unrelated.value=${UNRELATED_VALUE}\\n");
        String command = "printf 'App has been compiled by a more recent version of the Java Runtime "
                + "(class file version 65.0), this version only recognizes class file versions up to 61.0\\n' >&2; exit 1";

        var session = new DiagnosticEngine().diagnose(new DiagnosticRequest(root, command, "", Map.of(), true,
                ProbeSafety.PASSIVE, Duration.ofSeconds(2), 100_000));

        assertThat(session.rootCauses()).isNotEmpty();
        String rootRule = session.hypotheses().stream()
                .filter(h -> h.id().equals(session.rootCauses().getFirst().hypothesisId()))
                .findFirst().orElseThrow().ruleId();
        assertThat(rootRule).isEqualTo("JVM-01");
        assertThat(session.hypotheses()).noneMatch(h -> h.ruleId().equals("CFG-01"));
    }

    @Test void missingDiagnosticInputIsUnknownRatherThanHealthy(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>unknown</artifactId><version>1</version></project>");

        var session = new DiagnosticEngine().diagnose(new DiagnosticRequest(root, "", "", Map.of(), true,
                ProbeSafety.PASSIVE, Duration.ofSeconds(2), 100_000));

        assertThat(session.failure().summary()).isEqualTo("NO FAILURE INPUT AVAILABLE");
        assertThat(session.rootCauses()).isEmpty();
    }

    @Test void unknownNonzeroCommandIsStillReportedAsAConfirmedFailure(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>broken</artifactId><version>1</version></project>");

        var session = new DiagnosticEngine().diagnose(new DiagnosticRequest(root,
                "printf 'custom compiler exploded\\n' >&2; exit 7", "", Map.of(), true,
                ProbeSafety.PASSIVE, Duration.ofSeconds(2), 100_000));

        assertThat(session.failure().summary()).isEqualTo("Command failed with exit code 7: custom compiler exploded");
        assertThat(session.rootCauses()).isEmpty();
    }

    @Test void correlatedFailedTransactionIsFailureEvidenceRegardlessOfRequestSource(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>runtime</artifactId><version>1</version></project>");
        Instant now = Instant.now();
        var transaction = new RuntimeTransactionGroup("SERVER", "POST", "/orders", 500, true, 1,
                now, now, "0123456789abcdef0123456789abcdef", "java.lang.IllegalArgumentException",
                "Validation failed for header x-api-key",
                "java.lang.IllegalArgumentException: Validation failed for header x-api-key\n"
                        + "\tat com.acme.OrderController.create(OrderController.java:42)",
                "", "", 10_000);
        var runtime = new JfrRuntimeObservation("local-jvm", 77, 4, 0, 0, Duration.ofSeconds(1), List.of(),
                1, 1, List.of(transaction), "outcome-aware", "");

        var session = new DiagnosticEngine().diagnose(new DiagnosticRequest(root, "", "", Map.of(), true,
                ProbeSafety.PASSIVE, Duration.ofSeconds(2), 100_000, runtime));

        assertThat(session.failure().summary()).contains("IllegalArgumentException", "Validation failed");
        assertThat(session.evidence()).anyMatch(e -> e.id().equals("E-RUNTIME-OUTCOME")
                && e.strength().name().equals("VERY_HIGH")
                && Boolean.TRUE.equals(e.metadata().get("outcomeCorrelated")));
    }

    @Test void uncorrelatedThrownExceptionRemainsCandidateNotConfirmedFailure(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>runtime</artifactId><version>1</version></project>");
        Instant now = Instant.now();
        var candidate = new RuntimeExceptionGroup("java.lang.IllegalArgumentException", "handled", 5,
                now, now, "worker", List.of(), false, true);
        var runtime = new JfrRuntimeObservation("local-jvm", 77, 5, 0, 0, Duration.ofSeconds(1),
                List.of(candidate), "");

        var session = new DiagnosticEngine().diagnose(new DiagnosticRequest(root, "", "", Map.of(), true,
                ProbeSafety.PASSIVE, Duration.ofSeconds(2), 100_000, runtime));

        assertThat(session.failure().summary()).isEqualTo("RUNTIME EXCEPTION CANDIDATES OBSERVED");
        assertThat(session.rootCauses()).isEmpty();
    }
}
