package dev.devdoctor.engine.observe;

import static org.assertj.core.api.Assertions.assertThat;

import dev.devdoctor.engine.security.SecretRedactor;
import java.nio.file.Files;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;

class JfrTransactionAnalyzerTest {
    @Test
    void aggregatesOneOrManyTransactionsByObservedOutcome() throws Exception {
        var target = Files.createTempFile("devdoctor-transactions-", ".jfr");
        try (var recording = new Recording()) {
            recording.enable("dev.devdoctor.Transaction");
            recording.start();
            emit(1, true, 500, "missing password=raw-secret");
            emit(1_000, false, 200, "");
            recording.stop();
            recording.dump(target);
        }
        try {
            var result = new JfrRuntimeAnalyzer(new SecretRedactor(), 50, 16)
                    .analyze(target, "test", 7);
            assertThat(result.transactionEvents()).isEqualTo(1_001);
            assertThat(result.failedTransactions()).isEqualTo(1);
            assertThat(result.hasCorrelatedFailure()).isTrue();
            assertThat(result.transactionGroups()).extracting(RuntimeTransactionGroup::count)
                    .containsExactlyInAnyOrder(1L, 1_000L);
            assertThat(result.transactionGroups().getFirst().exceptionMessage())
                    .doesNotContain("raw-secret").contains("[REDACTED]");
        } finally {
            Files.deleteIfExists(target);
        }
    }

    private void emit(int count, boolean failed, long status, String exceptionMessage) {
        for (int index = 0; index < count; index++) {
            TestTransactionEvent event = new TestTransactionEvent();
            event.traceId = "0123456789abcdef0123456789abcdef";
            event.spanId = "0123456789abcdef";
            event.parentSpanId = "0000000000000000";
            event.spanKind = "SERVER";
            event.spanName = "GET /test";
            event.httpMethod = "GET";
            event.httpRoute = "/test";
            event.httpStatus = status;
            event.telemetryStatus = failed ? "ERROR" : "UNSET";
            event.failed = failed;
            event.exceptionType = failed ? "java.lang.IllegalStateException" : "";
            event.exceptionMessage = exceptionMessage;
            event.exceptionStack = failed ? "java.lang.IllegalStateException: " + exceptionMessage : "";
            event.databaseSystem = "";
            event.serverAddress = "";
            event.durationNanos = 1_000;
            event.commit();
        }
    }

    @Name("dev.devdoctor.Transaction")
    static final class TestTransactionEvent extends Event {
        String traceId;
        String spanId;
        String parentSpanId;
        String spanKind;
        String spanName;
        String httpMethod;
        String httpRoute;
        long httpStatus;
        String telemetryStatus;
        boolean failed;
        String exceptionType;
        String exceptionMessage;
        String exceptionStack;
        String databaseSystem;
        String serverAddress;
        long durationNanos;
    }
}
