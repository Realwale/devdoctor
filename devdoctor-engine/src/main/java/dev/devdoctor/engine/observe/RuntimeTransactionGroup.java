package dev.devdoctor.engine.observe;

import java.time.Instant;

/** Sanitized aggregate of application transactions with an observed outcome. */
public record RuntimeTransactionGroup(String spanKind, String method, String route, long statusCode,
                                      boolean failed, long count, Instant firstSeen, Instant lastSeen,
                                      String representativeTraceId, String exceptionClass,
                                      String exceptionMessage, String exceptionStackTrace,
                                      String databaseSystem, String serverAddress, long maximumDurationNanos) {
    public RuntimeTransactionGroup {
        spanKind = safe(spanKind);
        method = safe(method);
        route = safe(route);
        statusCode = Math.max(0, statusCode);
        count = Math.max(0, count);
        representativeTraceId = safe(representativeTraceId);
        exceptionClass = safe(exceptionClass);
        exceptionMessage = safe(exceptionMessage);
        exceptionStackTrace = safe(exceptionStackTrace);
        databaseSystem = safe(databaseSystem);
        serverAddress = safe(serverAddress);
        maximumDurationNanos = Math.max(0, maximumDurationNanos);
    }

    public boolean serverTransaction() { return "SERVER".equals(spanKind); }
    public boolean hasException() { return !exceptionClass.isBlank() || !exceptionStackTrace.isBlank(); }
    private static String safe(String value) { return value == null ? "" : value; }
}
