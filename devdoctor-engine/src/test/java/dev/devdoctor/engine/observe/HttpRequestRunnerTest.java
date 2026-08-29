package dev.devdoctor.engine.observe;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.devdoctor.engine.HttpRequestSpec;
import dev.devdoctor.engine.security.SecretRedactor;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpRequestRunnerTest {
    @Test void capturesHttpFailureWithoutRetainingRequestSecrets() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/orders", exchange -> {
            method.set(exchange.getRequestMethod());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String response = "request failed; auth=header-secret; parsed body value=body-secret; api=query-secret";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Set-Cookie", "session=response-secret");
            exchange.sendResponseHeaders(500, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/orders?api_key=query-secret");
            var spec = new HttpRequestSpec(uri, "POST", Map.of("Authorization", "Bearer header-secret"),
                    "{\"password\":\"body-secret\"}", 200, 299);

            HttpCapture capture = new HttpRequestRunner(new SecretRedactor(), Duration.ofSeconds(2), 100_000).run(spec);

            assertThat(capture.failed()).isTrue();
            assertThat(capture.statusCode()).isEqualTo(500);
            assertThat(capture.sanitizedUri()).endsWith("/orders").doesNotContain("query-secret", "api_key");
            assertThat(capture.requestHeaderNames()).containsExactly("Authorization");
            assertThat(capture.responseHeaderNames()).contains("set-cookie");
            assertThat(capture.responseBody()).doesNotContain("header-secret", "body-secret", "query-secret");
            assertThat(method).hasValue("POST");
            assertThat(authorization).hasValue("Bearer header-secret");
            assertThat(requestBody).hasValue("{\"password\":\"body-secret\"}");
        } finally {
            server.stop(0);
        }
    }
}
