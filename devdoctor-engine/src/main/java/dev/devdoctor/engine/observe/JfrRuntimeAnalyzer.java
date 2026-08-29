package dev.devdoctor.engine.observe;

import dev.devdoctor.engine.security.SecretRedactor;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;

/** Streams JFR exception events into bounded, redacted aggregates. */
public final class JfrRuntimeAnalyzer {
    private static final List<String> FRAMEWORK_PREFIXES = List.of("java.", "javax.", "jakarta.", "jdk.",
            "sun.", "org.springframework.", "reactor.", "io.netty.", "org.apache.", "com.fasterxml.");
    private final SecretRedactor redactor;
    private final int maximumGroups;
    private final int maximumFrames;

    public JfrRuntimeAnalyzer(SecretRedactor redactor, int maximumGroups, int maximumFrames) {
        this.redactor = redactor;
        this.maximumGroups = Math.max(1, maximumGroups);
        this.maximumFrames = Math.max(1, maximumFrames);
    }

    public JfrRuntimeObservation analyze(Path recording, String source, long processId) throws IOException {
        Map<String, MutableGroup> groups = new LinkedHashMap<>();
        Map<String, MutableTransactionGroup> transactions = new LinkedHashMap<>();
        long exceptionEvents = 0;
        long errorEvents = 0;
        long droppedGroups = 0;
        long transactionEvents = 0;
        long failedTransactions = 0;
        Instant first = null;
        Instant last = null;
        try (RecordingFile events = new RecordingFile(recording)) {
            while (events.hasMoreEvents()) {
                RecordedEvent event = events.readEvent();
                String eventName = event.getEventType().getName();
                if (eventName.equals("dev.devdoctor.Transaction")) {
                    transactionEvents++;
                    boolean failed = booleanValue(event, "failed");
                    if (failed) failedTransactions++;
                    Instant timestamp = event.getStartTime();
                    if (first == null || timestamp.isBefore(first)) first = timestamp;
                    if (last == null || timestamp.isAfter(last)) last = timestamp;
                    if (!addTransaction(transactions, event, timestamp, failed)) droppedGroups++;
                    continue;
                }
                boolean error = eventName.equals("jdk.JavaErrorThrow");
                if (!error && !eventName.equals("jdk.JavaExceptionThrow")) continue;
                if (error) errorEvents++; else exceptionEvents++;
                Instant timestamp = event.getStartTime();
                if (first == null || timestamp.isBefore(first)) first = timestamp;
                if (last == null || timestamp.isAfter(last)) last = timestamp;
                String exceptionClass = className(event);
                String message = redactor.redact(stringValue(event, "message"));
                List<StackFrame> frames = frames(event.getStackTrace());
                String applicationFrame = frames.stream().filter(StackFrame::applicationFrame)
                        .map(frame -> frame.className() + "." + frame.method()).findFirst().orElse("");
                String key = exceptionClass + '\n' + message + '\n' + applicationFrame;
                MutableGroup group = groups.get(key);
                if (group == null) {
                    if (groups.size() >= maximumGroups) { droppedGroups++; continue; }
                    String thread = event.getThread() == null ? "" : redactor.redact(event.getThread().getJavaName());
                    group = new MutableGroup(exceptionClass, message, timestamp, timestamp, thread, frames, error,
                            !applicationFrame.isBlank(), 0);
                    groups.put(key, group);
                }
                group.count++;
                group.lastSeen = timestamp;
            }
        }
        List<RuntimeExceptionGroup> result = groups.values().stream().map(MutableGroup::immutable)
                .sorted(Comparator.comparing(RuntimeExceptionGroup::error).reversed()
                        .thenComparing(Comparator.comparing(RuntimeExceptionGroup::applicationFramePresent).reversed())
                        .thenComparing(Comparator.comparingLong(RuntimeExceptionGroup::count).reversed())
                        .thenComparing(RuntimeExceptionGroup::exceptionClass))
                .toList();
        Duration duration = first == null || last == null ? Duration.ZERO : Duration.between(first, last);
        List<RuntimeTransactionGroup> transactionResult = transactions.values().stream()
                .map(MutableTransactionGroup::immutable)
                .sorted(Comparator.comparing(RuntimeTransactionGroup::failed).reversed()
                        .thenComparing(Comparator.comparing(RuntimeTransactionGroup::serverTransaction).reversed())
                        .thenComparing(Comparator.comparingLong(RuntimeTransactionGroup::count).reversed()))
                .toList();
        return new JfrRuntimeObservation(source, processId, exceptionEvents, errorEvents, droppedGroups, duration,
                result, transactionEvents, failedTransactions, transactionResult,
                transactionEvents > 0 ? "outcome-aware" : "generic-jfr", "");
    }

    private boolean addTransaction(Map<String, MutableTransactionGroup> transactions, RecordedEvent event,
                                   Instant timestamp, boolean failed) {
        String kind = redactor.redact(stringValue(event, "spanKind"));
        String method = redactor.redact(stringValue(event, "httpMethod"));
        String route = redactor.redact(stringValue(event, "httpRoute"));
        long status = longValue(event, "httpStatus");
        String exceptionClass = redactor.redact(stringValue(event, "exceptionType"));
        String message = redactor.redact(stringValue(event, "exceptionMessage"));
        String stack = redactor.redact(stringValue(event, "exceptionStack"));
        String database = redactor.redact(stringValue(event, "databaseSystem"));
        String server = redactor.redact(stringValue(event, "serverAddress"));
        String key = kind + '\n' + method + '\n' + route + '\n' + status + '\n' + failed + '\n'
                + exceptionClass + '\n' + message + '\n' + database + '\n' + server;
        MutableTransactionGroup group = transactions.get(key);
        if (group == null) {
            if (transactions.size() >= maximumGroups) return false;
            group = new MutableTransactionGroup(kind, method, route, status, failed, timestamp, timestamp,
                    redactor.redact(stringValue(event, "traceId")), exceptionClass, message, stack,
                    database, server, longValue(event, "durationNanos"));
            transactions.put(key, group);
        }
        group.count++;
        group.lastSeen = timestamp;
        group.maximumDurationNanos = Math.max(group.maximumDurationNanos, longValue(event, "durationNanos"));
        return true;
    }

    private String className(RecordedEvent event) {
        try {
            RecordedClass type = event.getClass("thrownClass");
            return type == null ? "java.lang.Throwable" : type.getName();
        } catch (IllegalArgumentException ignored) {
            return "java.lang.Throwable";
        }
    }

    private String stringValue(RecordedEvent event, String field) {
        try { return event.getString(field); }
        catch (IllegalArgumentException ignored) { return ""; }
    }

    private long longValue(RecordedEvent event, String field) {
        try { return event.getLong(field); }
        catch (IllegalArgumentException ignored) { return 0; }
    }

    private boolean booleanValue(RecordedEvent event, String field) {
        try { return event.getBoolean(field); }
        catch (IllegalArgumentException ignored) { return false; }
    }

    private List<StackFrame> frames(RecordedStackTrace trace) {
        if (trace == null) return List.of();
        List<StackFrame> frames = new ArrayList<>();
        for (RecordedFrame frame : trace.getFrames()) {
            if (frames.size() >= maximumFrames) break;
            String className = frame.getMethod().getType().getName();
            String method = frame.getMethod().getName();
            String simple = className.substring(className.lastIndexOf('.') + 1);
            int line = frame.getLineNumber();
            boolean application = FRAMEWORK_PREFIXES.stream().noneMatch(className::startsWith);
            frames.add(new StackFrame(redactor.redact(className), redactor.redact(method), simple + ".java",
                    line > 0 ? line : null, application));
        }
        return List.copyOf(frames);
    }

    private static final class MutableGroup {
        private final String exceptionClass;
        private final String message;
        private final Instant firstSeen;
        private Instant lastSeen;
        private final String thread;
        private final List<StackFrame> frames;
        private final boolean error;
        private final boolean applicationFrame;
        private long count;

        private MutableGroup(String exceptionClass, String message, Instant firstSeen, Instant lastSeen,
                             String thread, List<StackFrame> frames, boolean error,
                             boolean applicationFrame, long count) {
            this.exceptionClass = exceptionClass;
            this.message = message;
            this.firstSeen = firstSeen;
            this.lastSeen = lastSeen;
            this.thread = thread;
            this.frames = frames;
            this.error = error;
            this.applicationFrame = applicationFrame;
            this.count = count;
        }

        private RuntimeExceptionGroup immutable() {
            return new RuntimeExceptionGroup(exceptionClass, message, count, firstSeen, lastSeen, thread, frames,
                    error, applicationFrame);
        }
    }

    private static final class MutableTransactionGroup {
        private final String kind;
        private final String method;
        private final String route;
        private final long status;
        private final boolean failed;
        private final Instant firstSeen;
        private Instant lastSeen;
        private final String traceId;
        private final String exceptionClass;
        private final String exceptionMessage;
        private final String exceptionStack;
        private final String databaseSystem;
        private final String serverAddress;
        private long maximumDurationNanos;
        private long count;

        private MutableTransactionGroup(String kind, String method, String route, long status, boolean failed,
                                        Instant firstSeen, Instant lastSeen, String traceId, String exceptionClass,
                                        String exceptionMessage, String exceptionStack, String databaseSystem,
                                        String serverAddress, long maximumDurationNanos) {
            this.kind = kind;
            this.method = method;
            this.route = route;
            this.status = status;
            this.failed = failed;
            this.firstSeen = firstSeen;
            this.lastSeen = lastSeen;
            this.traceId = traceId;
            this.exceptionClass = exceptionClass;
            this.exceptionMessage = exceptionMessage;
            this.exceptionStack = exceptionStack;
            this.databaseSystem = databaseSystem;
            this.serverAddress = serverAddress;
            this.maximumDurationNanos = maximumDurationNanos;
        }

        private RuntimeTransactionGroup immutable() {
            return new RuntimeTransactionGroup(kind, method, route, status, failed, count, firstSeen, lastSeen,
                    traceId, exceptionClass, exceptionMessage, exceptionStack, databaseSystem, serverAddress,
                    maximumDurationNanos);
        }
    }
}
