package dev.devdoctor.engine.observe;

import java.time.Instant;
import java.util.Map;

public record LogEvent(Instant timestamp, String level, String thread, String message, Map<String, String> correlations) {
    public LogEvent { correlations = correlations == null ? Map.of() : Map.copyOf(correlations); }
}
