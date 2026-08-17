package dev.devdoctor.engine.report;

import dev.devdoctor.core.model.*;
import java.io.PrintWriter;

public final class TerminalReport {
    public void write(DiagnosticSession session, PrintWriter out, boolean verbose) {
        out.println("DEVDOCTOR"); out.println("Diagnostic ID: " + session.diagnosticId()); out.println();
        section(out, "FAILURE"); out.println(session.failure().summary());
        if (session.rootCauses().isEmpty()) {
            out.println(); out.println(session.failure().summary().equals("NO FAILURE DETECTED") ? "NO FAILURE DETECTED" : "NO HIGH-CONFIDENCE ROOT CAUSE FOUND");
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
    private void section(PrintWriter out, String title) { out.println(); out.println("=============================================="); out.println(title); out.println("=============================================="); }
}
