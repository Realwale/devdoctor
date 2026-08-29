package dev.devdoctor.engine.report;

import dev.devdoctor.core.model.*;
import java.io.PrintWriter;

public final class TerminalReport {
    public void write(DiagnosticSession session, PrintWriter out, boolean verbose) {
        out.println("DEVDOCTOR"); out.println("Diagnostic ID: " + session.diagnosticId()); out.println();
        boolean noFailure = session.failure().summary().equals("NO FAILURE REPRODUCED");
        boolean noInput = session.failure().summary().equals("NO FAILURE INPUT AVAILABLE");
        section(out, noFailure || noInput ? "OBSERVED RESULT" : "FAILURE"); out.println(session.failure().summary());
        if (session.rootCauses().isEmpty()) {
            out.println();
            if (noInput) {
                out.println("DevDoctor did not run a command or find a diagnostic log.");
                out.println("Pass --command, --log, or --url to reproduce the actual failing behavior.");
            } else if (noFailure && hasAutomaticBuild(session)) {
                out.println("BUILD/TEST CHECK PASSED");
                out.println("The detected Maven/Gradle test command completed successfully.");
                out.println("Runtime and API behavior were not exercised; this is not evidence that the application is healthy.");
                out.println("Replay the failing Postman request with --url, --method, --header and --data/--data-file.");
            } else if (noFailure && hasHttpEvidence(session)) {
                out.println("The supplied HTTP request returned an expected status.");
                out.println("This conclusion applies only to that request and does not certify the whole application.");
            } else if (noFailure) {
                out.println("No failure was reproduced by the supplied command or log evidence.");
                out.println("This conclusion applies only to the observed input, not the whole application.");
            } else {
                out.println("FAILURE CONFIRMED, BUT NO HIGH-CONFIDENCE ROOT CAUSE FOUND");
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
    private boolean hasAutomaticBuild(DiagnosticSession session) {
        return session.evidence().stream().anyMatch(e -> e.type() == EvidenceType.COMMAND
                && "AUTOMATIC_BUILD".equals(e.metadata().get("origin")));
    }
    private boolean hasHttpEvidence(DiagnosticSession session) {
        return session.evidence().stream().anyMatch(e -> e.type() == EvidenceType.HTTP);
    }
    private void section(PrintWriter out, String title) { out.println(); out.println("=============================================="); out.println(title); out.println("=============================================="); }
}
