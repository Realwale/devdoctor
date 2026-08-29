package dev.devdoctor.engine.reasoning;

import dev.devdoctor.core.model.*;
import dev.devdoctor.engine.knowledge.DiagnosticSignature;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SignatureRuleEngine {
    public List<MatchedHypothesis> evaluate(List<DiagnosticSignature> signatures, List<Evidence> evidence) {
        List<MatchedHypothesis> result = new ArrayList<>(); int sequence = 1;
        Set<String> observedFailureIds = new LinkedHashSet<>();
        evidence.stream().filter(this::isFailureObservation).map(Evidence::id).forEach(observedFailureIds::add);
        for (DiagnosticSignature signature : signatures) {
            Set<String> matchedIds = new LinkedHashSet<>(); boolean matches = true;
            for (String expression : signature.requiredPatterns()) {
                Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                List<Evidence> matched = evidence.stream().filter(this::isDiagnosticInput).filter(e -> pattern.matcher(searchText(e)).find()).toList();
                if (matched.isEmpty()) { matches = false; break; }
                matched.forEach(e -> matchedIds.add(e.id()));
            }
            boolean linkedToObservedFailure = matchedIds.stream().anyMatch(observedFailureIds::contains);
            if (matches && linkedToObservedFailure) {
                String hypothesisId = "H-" + String.format(Locale.ROOT, "%03d", sequence++);
                Hypothesis h = new Hypothesis(hypothesisId, signature.id(), signature.title(), signature.description(),
                        HypothesisStatus.UNTESTED, Confidence.VERY_LOW, List.copyOf(matchedIds), List.of(), signature.probeIds(), List.of());
                result.add(new MatchedHypothesis(h, signature));
            }
        }
        return List.copyOf(result);
    }

    private String searchText(Evidence evidence) { return evidence.summary() + " " + evidence.metadata(); }
    private boolean isFailureObservation(Evidence evidence) {
        if (evidence.type() == EvidenceType.LOG || evidence.type() == EvidenceType.STACK_TRACE) return true;
        if (evidence.type() == EvidenceType.HTTP) {
            Object status = evidence.metadata().get("statusCode");
            Object minimum = evidence.metadata().get("expectedStatusMin");
            Object maximum = evidence.metadata().get("expectedStatusMax");
            return Boolean.TRUE.equals(evidence.metadata().get("timedOut"))
                    || !String.valueOf(evidence.metadata().getOrDefault("error", "")).isBlank()
                    || status instanceof Number actual && minimum instanceof Number min && maximum instanceof Number max
                        && (actual.intValue() < min.intValue() || actual.intValue() > max.intValue());
        }
        if (evidence.type() != EvidenceType.COMMAND) return false;
        Object exitCode = evidence.metadata().get("exitCode");
        return Boolean.TRUE.equals(evidence.metadata().get("timedOut"))
                || exitCode instanceof Number number && number.intValue() != 0;
    }
    private boolean isDiagnosticInput(Evidence evidence) {
        return switch (evidence.type()) {
            case COMMAND, HTTP, STACK_TRACE, LOG, CONFIGURATION, ENVIRONMENT, DEPENDENCY, PORT, DNS, TCP, DATABASE, REDIS, DOCKER, JVM -> true;
            default -> false;
        };
    }
    public record MatchedHypothesis(Hypothesis hypothesis, DiagnosticSignature signature) {}
}
