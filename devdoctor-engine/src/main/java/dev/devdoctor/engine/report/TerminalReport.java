package dev.devdoctor.engine.report;

import dev.devdoctor.core.model.*;
import dev.devdoctor.engine.observe.RuntimeExceptionGroup;
import dev.devdoctor.engine.observe.RuntimeTransactionGroup;
import java.io.PrintWriter;
import java.util.List;

public final class TerminalReport {
    public void write(DiagnosticSession session, PrintWriter out, boolean verbose) {
        out.println("DEVDOCTOR"); out.println("Diagnostic ID: " + session.diagnosticId()); out.println();
        boolean noFailure = session.failure().summary().equals("NO FAILURE REPRODUCED");
        boolean noInput = session.failure().summary().equals("NO FAILURE INPUT AVAILABLE");
        boolean runtimeCandidates = session.failure().summary().equals("RUNTIME EXCEPTION CANDIDATES OBSERVED");
        boolean runtimeFailed = session.failure().summary().startsWith("RUNTIME OBSERVATION FAILED");
        section(out, noFailure || noInput || runtimeCandidates || runtimeFailed ? "OBSERVED RESULT" : "FAILURE");
        out.println(session.failure().summary());
        if (session.rootCauses().isEmpty()) {
            out.println();
            if (noInput) {
                out.println("DevDoctor did not run a command or find a diagnostic log.");
                out.println("Run while the application JVM is active, select it with --pid, import --recording, or pass --log/--command.");
            } else if (runtimeFailed) {
                out.println("DevDoctor could not access the selected runtime evidence.");
                out.println("Verify the PID belongs to an accessible local JVM, or import a readable JFR recording.");
            } else if (runtimeCandidates) {
                printRuntimeCandidates(session, out);
            } else if (noFailure && hasRuntimeEvidence(session)) {
                if (hasOutcomeEvidence(session)) {
                    out.println("No failed transaction outcome was observed in the selected JVM recording window.");
                } else {
                    out.println("No exception or error event was observed in the selected JVM recording window.");
                }
                out.println("This conclusion applies only to that window and does not certify the whole application.");
            } else if (noFailure) {
                out.println("No failure was reproduced by the supplied command or log evidence.");
                out.println("This conclusion applies only to the observed input, not the whole application.");
            } else {
                out.println("FAILURE CONFIRMED, BUT NO HIGH-CONFIDENCE ROOT CAUSE FOUND");
                printFailedTransactions(session, out);
            }
            if (!session.hypotheses().isEmpty()) { out.println(); out.println("Leading hypotheses:"); session.hypotheses().stream().limit(3).forEach(h -> out.println("  " + h.id() + " " + h.title() + " - " + h.status() + " / " + h.confidence())); }
        } else {
            section(out, "ROOT CAUSE"); RootCauseCandidate root = session.rootCauses().getFirst(); out.println(root.title()); out.println("Confidence: " + root.confidence());
            section(out, "SUPPORTING EVIDENCE"); root.evidencePath().forEach(id -> session.evidence().stream().filter(e -> e.id().equals(id)).findFirst().ifPresent(e -> out.println(id + "  " + e.summary() + " [" + e.strength() + "]")));
            section(out, "HYPOTHESES TESTED"); session.hypotheses().forEach(h -> out.println(h.id() + "  " + h.title() + " - " + h.status()));
            section(out, "RECOMMENDED REMEDIATION"); session.remediations().forEach(r -> out.println(r.description()));
            section(out, "VERIFY"); session.verification().forEach(v -> out.println("[ ] " + v.description()));
        }
        if (verbose) { section(out, "PROBES"); session.probes().forEach(p -> out.println(p.probeId() + "  " + p.status() + " - " + p.summary())); out.println(); out.println("Graph: " + session.graph().nodes().size() + " nodes, " + session.graph().edges().size() + " edges"); }
        out.flush();
    }
    private boolean hasRuntimeEvidence(DiagnosticSession session) {
        return session.evidence().stream().anyMatch(e -> e.id().equals("E-JVM-RUNTIME"));
    }
    private boolean hasOutcomeEvidence(DiagnosticSession session) {
        return session.evidence().stream().filter(e -> e.id().equals("E-JVM-RUNTIME"))
                .anyMatch(e -> Boolean.TRUE.equals(e.metadata().get("outcomeCorrelated")));
    }
    private void printFailedTransactions(DiagnosticSession session, PrintWriter out) {
        session.evidence().stream().filter(e -> e.id().equals("E-RUNTIME-OUTCOME")).findFirst().ifPresent(runtime -> {
            Object groups = runtime.metadata().get("transactionGroups");
            if (groups instanceof List<?> list) {
                out.println();
                out.println("Observed failed transactions:");
                list.stream().filter(RuntimeTransactionGroup.class::isInstance)
                        .map(RuntimeTransactionGroup.class::cast).limit(5)
                        .forEach(group -> out.println("  " + group.count() + "x " + group.method() + " "
                                + group.route() + (group.statusCode() == 0 ? "" : " -> HTTP " + group.statusCode())
                                + (group.exceptionClass().isBlank() ? "" : " (" + group.exceptionClass() + ")")));
            }
        });
    }
    private void printRuntimeCandidates(DiagnosticSession session, PrintWriter out) {
        Evidence runtime = session.evidence().stream().filter(e -> e.id().equals("E-JVM-RUNTIME"))
                .findFirst().orElseThrow();
        out.println("The JVM threw " + runtime.metadata().get("exceptionEvents") + " exceptions and "
                + runtime.metadata().get("errorEvents") + " errors during the observation window.");
        out.println("Thrown exceptions can be caught intentionally, so they are candidates—not confirmed request failures.");
        Object groups = runtime.metadata().get("exceptionGroups");
        if (groups instanceof List<?> list) {
            list.stream().filter(RuntimeExceptionGroup.class::isInstance).map(RuntimeExceptionGroup.class::cast)
                    .limit(5).forEach(group -> out.println("  " + group.count() + "x " + group.exceptionClass()
                            + (group.message().isBlank() ? "" : ": " + group.message())));
        }
        out.println("Correlate these candidates with application logs or outcome-aware runtime instrumentation before assigning root cause.");
    }
    private void section(PrintWriter out, String title) { out.println(); out.println("=============================================="); out.println(title); out.println("=============================================="); }
}
