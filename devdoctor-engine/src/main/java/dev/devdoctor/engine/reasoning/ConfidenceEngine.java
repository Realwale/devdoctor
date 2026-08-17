package dev.devdoctor.engine.reasoning;

import dev.devdoctor.core.model.*;
import dev.devdoctor.engine.knowledge.DiagnosticSignature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ConfidenceEngine {
    public Hypothesis assess(Hypothesis hypothesis, DiagnosticSignature signature, List<Evidence> allEvidence, List<ProbeResult> probes) {
        Map<String,Evidence> evidence = allEvidence.stream().collect(Collectors.toMap(Evidence::id, Function.identity(), (a,b) -> a));
        int score = 0; List<String> factors = new ArrayList<>();
        List<Evidence> supporting = hypothesis.supportingEvidence().stream().map(evidence::get).filter(java.util.Objects::nonNull).toList();
        if (!supporting.isEmpty()) { score += 3; factors.add("direct observation (+3)"); }
        long sources = supporting.stream().map(e -> e.source().kind() + ":" + e.source().locator()).distinct().count();
        if (sources > 1) { int added = (int) Math.min(4, (sources - 1) * 2); score += added; factors.add("independent supporting evidence (+" + added + ")"); }
        if (signature.requiredPatterns().size() > 1) { score += 1; factors.add("specific multi-signal rule (+1)"); }
        if (signature.causalLinkage()) { score += 2; factors.add("causal linkage (+2)"); }
        java.util.Set<String> linkedEvidence = new java.util.HashSet<>(hypothesis.supportingEvidence());
        linkedEvidence.addAll(hypothesis.contradictingEvidence());
        long completedProbes = probes.stream().filter(p -> hypothesis.availableProbes().contains(p.probeId()))
                .filter(p -> p.status() == ProbeStatus.SUCCEEDED || p.status() == ProbeStatus.FAILED)
                .filter(p -> p.evidence().stream().map(Evidence::id).anyMatch(linkedEvidence::contains)).count();
        if (completedProbes > 0) { score += 3; factors.add("targeted probe completed (+3)"); }
        score -= hypothesis.contradictingEvidence().size() * 3;
        if (!hypothesis.contradictingEvidence().isEmpty()) factors.add("contradicting evidence (-" + hypothesis.contradictingEvidence().size() * 3 + ")");
        Confidence confidence = category(score);
        HypothesisStatus status = hypothesis.contradictingEvidence().isEmpty()
                ? (confidence.ordinal() >= Confidence.HIGH.ordinal() ? HypothesisStatus.SUPPORTED : HypothesisStatus.WEAKLY_SUPPORTED)
                : (score <= 0 ? HypothesisStatus.RULED_OUT : HypothesisStatus.CONTRADICTED);
        return hypothesis.assessed(status, confidence, hypothesis.supportingEvidence(), hypothesis.contradictingEvidence(), factors);
    }

    private Confidence category(int score) {
        if (score <= 0) return Confidence.VERY_LOW;
        if (score <= 2) return Confidence.LOW;
        if (score <= 4) return Confidence.MEDIUM;
        if (score <= 7) return Confidence.HIGH;
        return Confidence.VERY_HIGH;
    }
}
