package dev.devdoctor.engine;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** An explicit HTTP interaction supplied by the user for runtime reproduction. */
public record HttpRequestSpec(URI uri, String method, Map<String, String> headers, String body,
                              int expectedStatusMin, int expectedStatusMax) {
    public HttpRequestSpec {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null
                || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("HTTP URL must use http or https and include a host");
        }
        method = method == null || method.isBlank() ? "GET" : method.trim().toUpperCase(Locale.ROOT);
        if (!method.matches("[A-Z]+")) throw new IllegalArgumentException("HTTP method contains invalid characters");
        Map<String, String> safeHeaders = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach((name, value) -> {
                String normalizedName = name == null ? "" : name.trim();
                String normalizedValue = value == null ? "" : value;
                if (!normalizedName.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) {
                    throw new IllegalArgumentException("Invalid HTTP header name: " + normalizedName);
                }
                if (normalizedValue.contains("\r") || normalizedValue.contains("\n")) {
                    throw new IllegalArgumentException("HTTP header values cannot contain newlines");
                }
                safeHeaders.put(normalizedName, normalizedValue);
            });
        }
        headers = Map.copyOf(safeHeaders);
        body = body == null ? "" : body;
        if (expectedStatusMin < 100 || expectedStatusMax > 599 || expectedStatusMin > expectedStatusMax) {
            throw new IllegalArgumentException("Expected HTTP status must be between 100 and 599");
        }
    }

    public boolean accepts(int statusCode) {
        return statusCode >= expectedStatusMin && statusCode <= expectedStatusMax;
    }
}
