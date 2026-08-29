package dev.devdoctor.engine.observe;

import java.time.Duration;
import java.util.List;

public record HttpCapture(String method, String sanitizedUri, int statusCode, int expectedStatusMin,
                          int expectedStatusMax, boolean timedOut, boolean truncated, Duration duration,
                          String responseBody, String error, List<String> requestHeaderNames,
                          List<String> responseHeaderNames) {
    public boolean failed() {
        return !error.isBlank() || timedOut || statusCode < expectedStatusMin || statusCode > expectedStatusMax;
    }
}
