package dev.devdoctor.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.devdoctor.core.json.DiagnosticJson;
import dev.devdoctor.core.model.ProbeSafety;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpDiagnosticEngineTest {
    @Test void diagnosesRuntimeConfigurationFailureFromHttpResponse(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>payments</artifactId><version>1</version></project>");
        Files.writeString(root.resolve("application.properties"), "payments.token=${PAYMENTS_TOKEN}\n");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/payments", exchange -> {
            byte[] response = "Required environment variable PAYMENTS_TOKEN is missing".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var http = new HttpRequestSpec(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/payments"),
                    "POST", Map.of("Authorization", "Bearer runtime-secret"), "{}", 200, 299);
            var request = new DiagnosticRequest(root, "", "", Map.of(), true, ProbeSafety.PASSIVE,
                    Duration.ofSeconds(2), 100_000, false, http);

            var session = new DiagnosticEngine().diagnose(request);

            assertThat(session.failure().summary()).startsWith("HTTP request returned status 500");
            assertThat(session.rootCauses()).isNotEmpty();
            String rootRule = session.hypotheses().stream()
                    .filter(h -> h.id().equals(session.rootCauses().getFirst().hypothesisId()))
                    .findFirst().orElseThrow().ruleId();
            assertThat(rootRule).isEqualTo("CFG-01");
            assertThat(session.evidence()).anyMatch(e -> e.id().equals("E-HTTP") && e.type().name().equals("HTTP"));
            assertThat(new DiagnosticJson().write(session)).doesNotContain("runtime-secret");
        } finally {
            server.stop(0);
        }
    }
}
