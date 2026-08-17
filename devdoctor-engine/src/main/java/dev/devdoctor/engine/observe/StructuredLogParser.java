package dev.devdoctor.engine.observe;

import dev.devdoctor.engine.security.SecretRedactor;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StructuredLogParser {
    private static final Pattern LINE = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T[^\\s]+)?\\s*(ERROR|WARN|INFO|DEBUG|TRACE)?\\s*(?:\\[([^]]+)])?\\s*(.*)$");
    private static final Pattern CORRELATION = Pattern.compile("(?i)\\b(traceId|spanId|requestId|transactionId)[=:]([A-Za-z0-9_-]+)");
    private final SecretRedactor redactor;
    public StructuredLogParser(SecretRedactor redactor) { this.redactor = redactor; }

    public List<LogEvent> parse(String text) {
        List<LogEvent> events = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            Matcher matcher = LINE.matcher(raw);
            if (!matcher.matches()) continue;
            String level = matcher.group(2);
            String message = matcher.group(4) == null ? "" : redactor.redact(matcher.group(4));
            if (level == null && !message.contains("Exception") && !message.contains("Error")) continue;
            Map<String, String> ids = new LinkedHashMap<>();
            Matcher idMatcher = CORRELATION.matcher(message);
            while (idMatcher.find()) ids.put(idMatcher.group(1).toLowerCase(), idMatcher.group(2));
            events.add(new LogEvent(parseInstant(matcher.group(1)), level == null ? "EXCEPTION" : level,
                    matcher.group(3) == null ? "" : matcher.group(3), message, ids));
        }
        return List.copyOf(events);
    }

    private Instant parseInstant(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); } catch (DateTimeParseException ignored) { return null; }
    }
}
