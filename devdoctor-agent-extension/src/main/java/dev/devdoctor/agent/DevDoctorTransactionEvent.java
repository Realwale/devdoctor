package dev.devdoctor.agent;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name("dev.devdoctor.Transaction")
@Label("DevDoctor Transaction Outcome")
@Category({"DevDoctor", "Diagnostics"})
@Description("Outcome-correlated server and dependency spans; never contains request or response bodies")
@StackTrace(false)
final class DevDoctorTransactionEvent extends Event {
    @Label("Trace ID") String traceId;
    @Label("Span ID") String spanId;
    @Label("Parent Span ID") String parentSpanId;
    @Label("Span Kind") String spanKind;
    @Label("Span Name") String spanName;
    @Label("HTTP Method") String httpMethod;
    @Label("HTTP Route") String httpRoute;
    @Label("HTTP Status") long httpStatus;
    @Label("OpenTelemetry Status") String telemetryStatus;
    @Label("Failed") boolean failed;
    @Label("Exception Type") String exceptionType;
    @Label("Exception Message") String exceptionMessage;
    @Label("Exception Stack") String exceptionStack;
    @Label("Database System") String databaseSystem;
    @Label("Server Address") String serverAddress;
    @Label("Duration (ns)") long durationNanos;
}
