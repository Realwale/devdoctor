package dev.devdoctor.engine.observe;

import dev.devdoctor.engine.HttpRequestSpec;
import dev.devdoctor.engine.security.SecretRedactor;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Executes only an HTTP request explicitly supplied by the user and retains sanitized evidence. */
public final class HttpRequestRunner {
    private final SecretRedactor redactor;
    private final Duration timeout;
    private final int outputLimit;

    public HttpRequestRunner(SecretRedactor redactor, Duration timeout, int outputLimit) {
        this.redactor = redactor;
        this.timeout = timeout;
        this.outputLimit = outputLimit;
    }

    public HttpCapture run(HttpRequestSpec spec) {
        Instant started = Instant.now();
        String safeUri = sanitizeUri(spec.uri());
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(spec.uri()).timeout(timeout);
            spec.headers().forEach(request::header);
            HttpRequest.BodyPublisher publisher = spec.body().isEmpty()
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(spec.body(), StandardCharsets.UTF_8);
            request.method(spec.method(), publisher);
            HttpClient client = HttpClient.newBuilder().connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpResponse<InputStream> response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            CapturedBody body;
            try (InputStream stream = response.body()) { body = capture(stream); }
            String safeBody = redactRequestEchoes(body.text(), spec);
            return new HttpCapture(spec.method(), safeUri, response.statusCode(), spec.expectedStatusMin(),
                    spec.expectedStatusMax(), false, body.truncated(), Duration.between(started, Instant.now()),
                    safeBody, "", spec.headers().keySet().stream().sorted().toList(),
                    response.headers().map().keySet().stream().sorted().toList());
        } catch (HttpTimeoutException failure) {
            return failed(spec, safeUri, started, true, failure);
        } catch (IOException failure) {
            return failed(spec, safeUri, started, false, failure);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            return failed(spec, safeUri, started, true, failure);
        } catch (IllegalArgumentException failure) {
            return failed(spec, safeUri, started, false, failure);
        }
    }

    private HttpCapture failed(HttpRequestSpec spec, String safeUri, Instant started, boolean timedOut, Exception failure) {
        String raw = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
        raw = raw.replace(spec.uri().toString(), safeUri);
        return new HttpCapture(spec.method(), safeUri, -1, spec.expectedStatusMin(), spec.expectedStatusMax(),
                timedOut, false, Duration.between(started, Instant.now()), "", redactor.redact(raw),
                spec.headers().keySet().stream().sorted().toList(), List.of());
    }

    private CapturedBody capture(InputStream stream) throws IOException {
        byte[] bytes = stream.readNBytes(outputLimit + 1);
        boolean truncated = bytes.length > outputLimit;
        int length = Math.min(bytes.length, outputLimit);
        return new CapturedBody(new String(bytes, 0, length, StandardCharsets.UTF_8), truncated);
    }

    private String redactRequestEchoes(String response, HttpRequestSpec spec) {
        String safe = redactor.redact(response);
        for (String value : spec.headers().values()) {
            safe = replaceSecret(safe, value);
            for (String component : value.split("[\\s,;]+")) safe = replaceSecret(safe, component);
        }
        safe = replaceSecret(safe, spec.body());
        for (String component : spec.body().split("[^A-Za-z0-9._~+/@:-]+")) {
            safe = replaceSecret(safe, component);
        }
        String query = spec.uri().getRawQuery();
        if (query != null) {
            for (String field : query.split("&")) {
                int equals = field.indexOf('=');
                if (equals < 0) continue;
                String raw = field.substring(equals + 1);
                safe = replaceSecret(safe, raw);
                try { safe = replaceSecret(safe, URLDecoder.decode(raw, StandardCharsets.UTF_8)); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        return safe;
    }

    private String replaceSecret(String text, String value) {
        return value != null && value.length() >= 4 ? text.replace(value, "[SECRET_REDACTED]") : text;
    }

    private String sanitizeUri(URI uri) {
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath(), null, null).toString();
        } catch (Exception ignored) {
            return uri.getScheme() + "://" + uri.getHost();
        }
    }

    private record CapturedBody(String text, boolean truncated) {}
}
