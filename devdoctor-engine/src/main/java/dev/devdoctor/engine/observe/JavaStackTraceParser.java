package dev.devdoctor.engine.observe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaStackTraceParser {
    private static final Pattern THREAD = Pattern.compile("Exception in thread \\\"([^\\\"]+)\\\"\\s+(.+)");
    private static final Pattern EXCEPTION = Pattern.compile("^(?:Caused by:\\s*|Suppressed:\\s*)?([\\w.$]+(?:Exception|Error|Throwable))(?::\\s*(.*))?$");
    private static final Pattern FRAME = Pattern.compile("^\\s*at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(([^:()]+)(?::(\\d+))?\\)");
    private static final List<String> FRAMEWORK_PREFIXES = List.of("java.", "javax.", "jdk.", "sun.", "org.springframework.", "reactor.", "io.netty.", "org.junit.", "org.apache.", "com.fasterxml.");

    public ParsedStackTrace parse(String text) {
        String thread = "";
        List<Builder> exceptions = new ArrayList<>();
        Builder current = null;
        for (String line : text.split("\\R")) {
            Matcher threadMatcher = THREAD.matcher(line);
            String candidate = line.trim();
            if (threadMatcher.matches()) { thread = threadMatcher.group(1); candidate = threadMatcher.group(2); }
            Matcher exceptionMatcher = EXCEPTION.matcher(candidate);
            if (exceptionMatcher.matches()) {
                current = new Builder(exceptionMatcher.group(1), nullToEmpty(exceptionMatcher.group(2)), candidate.startsWith("Suppressed:"));
                exceptions.add(current);
                continue;
            }
            Matcher frameMatcher = FRAME.matcher(line);
            if (current != null && frameMatcher.find()) {
                String className = frameMatcher.group(1);
                Integer lineNumber = frameMatcher.group(4) == null ? null : Integer.valueOf(frameMatcher.group(4));
                boolean application = FRAMEWORK_PREFIXES.stream().noneMatch(className::startsWith);
                current.frames.add(new StackFrame(className, frameMatcher.group(2), frameMatcher.group(3), lineNumber, application));
            }
        }
        return new ParsedStackTrace(thread, exceptions.stream().map(Builder::build).toList());
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }
    private static final class Builder {
        final String type; final String message; final boolean suppressed; final List<StackFrame> frames = new ArrayList<>();
        Builder(String type, String message, boolean suppressed) { this.type = type; this.message = message; this.suppressed = suppressed; }
        ParsedException build() { return new ParsedException(type, message, frames, suppressed); }
    }
}
