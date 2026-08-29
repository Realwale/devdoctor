package dev.devdoctor.engine.observe;

import java.time.Duration;
import java.util.List;

/** Sanitized, bounded JVM evidence captured locally or imported from another server. */
public record JfrRuntimeObservation(String source, long processId, long exceptionEvents, long errorEvents,
                                    long droppedGroups, Duration duration,
                                    List<RuntimeExceptionGroup> exceptionGroups,
                                    long transactionEvents, long failedTransactions,
                                    List<RuntimeTransactionGroup> transactionGroups,
                                    String instrumentationStatus, String captureError) {
    public JfrRuntimeObservation {
        source = source == null ? "" : source;
        processId = Math.max(0, processId);
        exceptionEvents = Math.max(0, exceptionEvents);
        errorEvents = Math.max(0, errorEvents);
        droppedGroups = Math.max(0, droppedGroups);
        duration = duration == null ? Duration.ZERO : duration;
        exceptionGroups = exceptionGroups == null ? List.of() : List.copyOf(exceptionGroups);
        transactionEvents = Math.max(0, transactionEvents);
        failedTransactions = Math.max(0, failedTransactions);
        transactionGroups = transactionGroups == null ? List.of() : List.copyOf(transactionGroups);
        instrumentationStatus = instrumentationStatus == null ? "" : instrumentationStatus;
        captureError = captureError == null ? "" : captureError;
    }

    public JfrRuntimeObservation(String source, long processId, long exceptionEvents, long errorEvents,
                                 long droppedGroups, Duration duration,
                                 List<RuntimeExceptionGroup> exceptionGroups, String captureError) {
        this(source, processId, exceptionEvents, errorEvents, droppedGroups, duration, exceptionGroups,
                0, 0, List.of(), "not-requested", captureError);
    }

    public boolean hasExceptionCandidates() {
        return exceptionEvents > 0 || errorEvents > 0;
    }

    public boolean hasCorrelatedFailure() { return failedTransactions > 0; }

    public boolean hasOutcomeEvidence() { return transactionEvents > 0; }
}
