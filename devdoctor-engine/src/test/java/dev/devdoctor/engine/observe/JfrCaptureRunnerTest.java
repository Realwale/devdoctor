package dev.devdoctor.engine.observe;

import static org.assertj.core.api.Assertions.assertThat;

import dev.devdoctor.engine.security.SecretRedactor;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JfrCaptureRunnerTest {
    @Test void capturesRuntimeExceptionsFromAnotherJvmWithoutGeneratingRequests() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process fixture = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                RuntimeFailureFixture.class.getName()).redirectErrorStream(true).start();
        try {
            Thread.sleep(500);

            JfrRuntimeObservation observation = new JfrCaptureRunner(new SecretRedactor(), 100_000, 100)
                    .capture(fixture.pid(), Duration.ofSeconds(2));

            assertThat(observation.captureError()).isBlank();
            assertThat(observation.processId()).isEqualTo(fixture.pid());
            assertThat(observation.exceptionEvents()).isGreaterThan(0);
            assertThat(observation.exceptionGroups()).anySatisfy(group -> {
                assertThat(group.exceptionClass()).isEqualTo(IllegalStateException.class.getName());
                assertThat(group.message()).contains("DEVDOCTOR_RUNTIME_TOKEN", "is missing");
                assertThat(group.applicationFramePresent()).isTrue();
                assertThat(group.frames()).anySatisfy(frame ->
                        assertThat(frame.className()).isEqualTo(RuntimeFailureFixture.class.getName()));
            });
        } finally {
            fixture.destroyForcibly();
            fixture.waitFor();
        }
    }
}
