package dev.devdoctor.engine.observe;

import java.time.Instant;
import java.util.List;

/** Aggregated exception evidence from a JVM recording. */
public record RuntimeExceptionGroup(String exceptionClass, String message, long count, Instant firstSeen,
                                    Instant lastSeen, String thread, List<StackFrame> frames,
                                    boolean error, boolean applicationFramePresent) {
    public RuntimeExceptionGroup {
        exceptionClass = exceptionClass == null ? "" : exceptionClass;
        message = message == null ? "" : message;
        count = Math.max(0, count);
        firstSeen = firstSeen == null ? Instant.EPOCH : firstSeen;
        lastSeen = lastSeen == null ? firstSeen : lastSeen;
        thread = thread == null ? "" : thread;
        frames = frames == null ? List.of() : List.copyOf(frames);
    }

    public String stackTraceText() {
        StringBuilder text = new StringBuilder();
        text.append("Exception in thread \"").append(thread.isBlank() ? "unknown" : thread).append("\" ")
                .append(exceptionClass);
        if (!message.isBlank()) text.append(": ").append(message);
        for (StackFrame frame : frames) {
            text.append("\n\tat ").append(frame.className()).append('.').append(frame.method()).append('(')
                    .append(frame.file() == null || frame.file().isBlank() ? "Unknown Source" : frame.file());
            if (frame.line() != null && frame.line() > 0) text.append(':').append(frame.line());
            text.append(')');
        }
        return text.toString();
    }
}
