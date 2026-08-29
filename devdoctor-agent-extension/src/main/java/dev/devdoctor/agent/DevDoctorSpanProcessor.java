package dev.devdoctor.agent;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Converts completed, correlated spans into local JFR evidence without exporting telemetry. */
public final class DevDoctorSpanProcessor implements SpanProcessor {
    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.request.method");
    private static final AttributeKey<String> HTTP_METHOD_OLD = AttributeKey.stringKey("http.method");
    private static final AttributeKey<String> HTTP_ROUTE = AttributeKey.stringKey("http.route");
    private static final AttributeKey<Long> HTTP_STATUS = AttributeKey.longKey("http.response.status_code");
    private static final AttributeKey<Long> HTTP_STATUS_OLD = AttributeKey.longKey("http.status_code");
    private static final AttributeKey<String> DB_SYSTEM = AttributeKey.stringKey("db.system.name");
    private static final AttributeKey<String> DB_SYSTEM_OLD = AttributeKey.stringKey("db.system");
    private static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");
    private static final AttributeKey<String> EXCEPTION_TYPE = AttributeKey.stringKey("exception.type");
    private static final AttributeKey<String> EXCEPTION_MESSAGE = AttributeKey.stringKey("exception.message");
    private static final AttributeKey<String> EXCEPTION_STACK = AttributeKey.stringKey("exception.stacktrace");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)(password|passwd|secret|token|api[-_.]?key|authorization|cookie)(\\s*[:=]\\s*)([^\\s,;]+)");

    @Override public void onStart(Context parentContext, ReadWriteSpan span) { }

    @Override public boolean isStartRequired() { return false; }

    @Override public void onEnd(ReadableSpan span) {
        SpanData data = span.toSpanData();
        boolean server = data.getKind() == SpanKind.SERVER;
        boolean failed = failed(data);
        if (!server && !failed) return;

        Attributes attributes = data.getAttributes();
        ExceptionFields exception = exception(data.getEvents());
        DevDoctorTransactionEvent event = new DevDoctorTransactionEvent();
        if (!event.isEnabled()) return;
        event.traceId = data.getTraceId();
        event.spanId = data.getSpanId();
        event.parentSpanId = data.getParentSpanId();
        event.spanKind = data.getKind().name();
        event.spanName = safe(data.getName(), 512);
        event.httpMethod = safe(first(attributes.get(HTTP_METHOD), attributes.get(HTTP_METHOD_OLD)), 32);
        event.httpRoute = safe(attributes.get(HTTP_ROUTE), 512);
        event.httpStatus = number(first(attributes.get(HTTP_STATUS), attributes.get(HTTP_STATUS_OLD)));
        event.telemetryStatus = data.getStatus().getStatusCode().name();
        event.failed = failed;
        event.exceptionType = safe(exception.type(), 512);
        event.exceptionMessage = safe(exception.message(), 2_048);
        event.exceptionStack = safe(exception.stack(), 16_384);
        event.databaseSystem = safe(first(attributes.get(DB_SYSTEM), attributes.get(DB_SYSTEM_OLD)), 128);
        event.serverAddress = safe(attributes.get(SERVER_ADDRESS), 512);
        event.durationNanos = Math.max(0, data.getEndEpochNanos() - data.getStartEpochNanos());
        event.commit();
    }

    @Override public boolean isEndRequired() { return true; }
    @Override public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
    @Override public CompletableResultCode forceFlush() { return CompletableResultCode.ofSuccess(); }

    private boolean failed(SpanData data) {
        if (data.getStatus().getStatusCode() == StatusCode.ERROR) return true;
        Long status = first(data.getAttributes().get(HTTP_STATUS), data.getAttributes().get(HTTP_STATUS_OLD));
        return status != null && status >= 400;
    }

    private ExceptionFields exception(List<EventData> events) {
        for (int i = events.size() - 1; i >= 0; i--) {
            EventData event = events.get(i);
            if (!"exception".equals(event.getName())) continue;
            Attributes attributes = event.getAttributes();
            return new ExceptionFields(attributes.get(EXCEPTION_TYPE), attributes.get(EXCEPTION_MESSAGE),
                    attributes.get(EXCEPTION_STACK));
        }
        return ExceptionFields.EMPTY;
    }

    private static <T> T first(T preferred, T fallback) { return preferred == null ? fallback : preferred; }
    private static long number(Long value) { return value == null ? 0 : Math.max(0, value); }

    private static String safe(String value, int maximum) {
        if (value == null) return "";
        String singleLine = value.replace('\u0000', ' ').replace("\r", "\\r");
        String redacted = SECRET_ASSIGNMENT.matcher(singleLine).replaceAll("$1$2[REDACTED]");
        return redacted.length() <= maximum ? redacted : redacted.substring(0, maximum) + "…";
    }

    private record ExceptionFields(String type, String message, String stack) {
        private static final ExceptionFields EMPTY = new ExceptionFields("", "", "");
    }
}
