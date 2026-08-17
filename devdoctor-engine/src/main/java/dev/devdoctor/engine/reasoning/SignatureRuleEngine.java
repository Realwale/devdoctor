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
        for (DiagnosticSignature signature : signatures) {
            Set<String> matchedIds = new LinkedHashSet<>(); boolean matches = true;
            for (String expression : signature.requiredPatterns()) {
                Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                List<Evidence> matched = evidence.stream().filter(this::isDiagnosticInput).filter(e -> pattern.matcher(searchText(e)).find()).toList();
                if (matched.isEmpty()) { matches = false; break; }
                matched.forEach(e -> matchedIds.add(e.id()));
            }
            if (matches) {
                String hypothesisId = "H-" + String.format(Locale.ROOT, "%03d", sequence++);
                Hypothesis h = new Hypothesis(hypothesisId, signature.id(), signature.title(), signature.description(),
                        HypothesisStatus.UNTESTED, Confidence.VERY_LOW, List.copyOf(matchedIds), List.of(), signature.probeIds(), List.of());
                result.add(new MatchedHypothesis(h, signature));
            }
        }
        return List.copyOf(result);
    }

    private String searchText(Evidence evidence) { return evidence.summary() + " " + evidence.metadata(); }
    private boolean isDiagnosticInput(Evidence evidence) {
        return switch (evidence.type()) {
            case COMMAND, STACK_TRACE, LOG, CONFIGURATION, ENVIRONMENT, DEPENDENCY, PORT, DNS, TCP, DATABASE, REDIS, DOCKER, JVM -> true;
            default -> false;
        };
    }
    public record MatchedHypothesis(Hypothesis hypothesis, DiagnosticSignature signature) {}
}
