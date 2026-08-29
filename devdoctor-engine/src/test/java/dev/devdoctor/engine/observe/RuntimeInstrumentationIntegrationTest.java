package dev.devdoctor.engine.observe;

import static org.assertj.core.api.Assertions.assertThat;

import dev.devdoctor.engine.security.SecretRedactor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class RuntimeInstrumentationIntegrationTest {
    @Test
    void attachesToAlreadyRunningJvmAndCorrelatesExternalTrafficOutcome() throws Exception {
        Path agent = java.util.stream.Stream.of(
                        Path.of("devdoctor-agent-extension", "target", "devdoctor-agent.jar"),
                        Path.of("..", "devdoctor-agent-extension", "target", "devdoctor-agent.jar"))
                .map(Path::toAbsolutePath).map(Path::normalize).filter(Files::isRegularFile)
                .findFirst().orElseThrow(() -> new AssertionError("Built DevDoctor agent not found"));
        assertThat(agent).isRegularFile();
        int port;
        try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process application = new ProcessBuilder(javaExecutable, "-javaagent:" + agent, "-cp",
                System.getProperty("java.class.path"), RuntimeSpringFixture.class.getName(), Integer.toString(port))
                .redirectErrorStream(true).start();
        try (var reader = new BufferedReader(new InputStreamReader(application.getInputStream(), StandardCharsets.UTF_8));
             var executor = Executors.newSingleThreadExecutor()) {
            String line;
            boolean ready = false;
            for (int index = 0; index < 50 && (line = reader.readLine()) != null; index++) {
                if (line.equals("READY")) { ready = true; break; }
            }
            assertThat(ready).as("instrumented Spring fixture reached READY").isTrue();
            var capture = executor.submit(() -> new JfrCaptureRunner(new SecretRedactor(), 1_000_000, 100)
                    .capture(application.pid(), Duration.ofSeconds(6)));
            Thread.sleep(3_000);
            var client = HttpClient.newHttpClient();
            for (int index = 0; index < 10; index++) {
                get(client, port, "/ok");
                get(client, port, "/fail");
                Thread.sleep(100);
            }
            JfrRuntimeObservation observation = capture.get();
            assertThat(observation.captureError()).isBlank();
            assertThat(observation.instrumentationStatus()).isEqualTo("outcome-aware");
            assertThat(observation.transactionEvents()).isGreaterThanOrEqualTo(20);
            assertThat(observation.failedTransactions()).isGreaterThanOrEqualTo(10);
            assertThat(observation.transactionGroups()).anyMatch(group -> group.failed() && group.statusCode() == 500);
        } finally {
            application.destroy();
            if (!application.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) application.destroyForcibly();
        }
    }

    private void get(HttpClient client, int port, String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
        client.send(request, HttpResponse.BodyHandlers.discarding());
    }
}
