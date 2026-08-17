package dev.devdoctor.engine.reasoning;

import dev.devdoctor.core.model.*;
import java.util.ArrayList;
import java.util.List;

public final class CorrelationEngine {
    public Hypothesis correlate(Hypothesis hypothesis, List<ProbeResult> probes) {
        List<String> support = new ArrayList<>(hypothesis.supportingEvidence());
        List<String> contradict = new ArrayList<>(hypothesis.contradictingEvidence());
        for (ProbeResult probe : probes) {
            if (!hypothesis.availableProbes().contains(probe.probeId()) || probe.status() == ProbeStatus.SKIPPED || probe.status() == ProbeStatus.DENIED) continue;
            if (probe.probeId().equals("environment-whitespace")) {
                boolean found = !((List<?>) probe.sanitizedDetails().getOrDefault("matchingEvidence", List.of())).isEmpty();
                if (found && List.of("HTTP-01", "CFG-04").contains(hypothesis.ruleId())) {
                    Object matches = probe.sanitizedDetails().get("matchingEvidence");
                    if (matches instanceof List<?> list) list.stream().filter(String.class::isInstance).map(String.class::cast).forEach(support::add);
                }
                continue;
            }
            if (probe.probeId().equals("port-owner")) {
                // A current owner supports a bind-conflict diagnosis. Its absence cannot disprove a historical failure.
                if (Boolean.TRUE.equals(probe.sanitizedDetails().get("occupied")) && hypothesis.ruleId().equals("INF-01")) {
                    probe.evidence().stream().map(Evidence::id).forEach(support::add);
                }
                continue;
            }
            if (probe.probeId().equals("environment-presence") && hypothesis.ruleId().equals("CFG-01")) {
                List<String> present = new ArrayList<>();
                Object presentValue = probe.sanitizedDetails().getOrDefault("presentEvidence", List.of());
                if (presentValue instanceof List<?> list) list.stream().filter(String.class::isInstance).map(String.class::cast).forEach(present::add);
                Object missing = probe.sanitizedDetails().getOrDefault("missing", List.of());
                if (missing instanceof List<?> list && list.isEmpty()) contradict.addAll(present);
                else support.addAll(present);
                continue;
            }
            if (probe.probeId().equals("dependency-version") && hypothesis.ruleId().equals("DEP-03")) {
                if (((Number) probe.sanitizedDetails().getOrDefault("explicitVersionDeclarations", 0)).intValue() > 0) {
                    probe.evidence().stream().map(Evidence::id).forEach(support::add);
                }
                continue;
            }
            Boolean expected = expectedFailure(hypothesis.ruleId(), probe.probeId());
            if (expected == null) continue;
            boolean failed = probe.status() == ProbeStatus.FAILED;
            List<String> ids = probe.evidence().stream().map(Evidence::id).toList();
            if (failed == expected) support.addAll(ids); else contradict.addAll(ids);
        }
        return hypothesis.assessed(hypothesis.status(), hypothesis.confidence(), support.stream().distinct().toList(), contradict.stream().distinct().toList(), hypothesis.confidenceFactors());
    }

    private Boolean expectedFailure(String rule, String probe) {
        if (probe.equals("dns-resolution")) {
            if (rule.equals("NET-01")) return true;
            if (List.of("DB-01", "NET-02", "NET-03", "HTTP-03", "INF-03").contains(rule)) return false;
            return null;
        }
        if (probe.equals("tcp-connectivity")) return List.of("DB-01", "NET-02", "NET-03", "HTTP-03", "INF-03").contains(rule) ? true : null;
        if (probe.equals("redis-reachability")) return rule.equals("INF-02") ? true : null;
        if (probe.equals("docker-daemon")) return rule.equals("INF-04") ? true : null;
        return null;
    }
}
