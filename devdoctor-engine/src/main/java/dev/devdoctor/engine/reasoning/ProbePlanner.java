package dev.devdoctor.engine.reasoning;

import dev.devdoctor.core.model.Hypothesis;
import dev.devdoctor.core.model.ProbeResult;
import dev.devdoctor.core.model.ProbeSafety;
import dev.devdoctor.core.model.ProbeStatus;
import dev.devdoctor.core.spi.DiagnosticContext;
import dev.devdoctor.core.spi.DiagnosticProbe;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProbePlanner {
    public List<ProbeResult> execute(List<Hypothesis> hypotheses, List<DiagnosticProbe> available, DiagnosticContext context) {
        Set<String> requested = new LinkedHashSet<>(); hypotheses.forEach(h -> requested.addAll(h.availableProbes()));
        List<ProbeResult> results = new ArrayList<>();
        available.stream().filter(p -> requested.contains(p.id())).sorted(Comparator.comparing(DiagnosticProbe::safety).thenComparing(DiagnosticProbe::id)).forEach(probe -> {
            if (probe.safety() == ProbeSafety.PROHIBITED || probe.safety().ordinal() > context.maximumSafety().ordinal()) {
                results.add(new ProbeResult(probe.id(), probe.safety(), ProbeStatus.DENIED, "Probe denied by safety policy", Instant.now(), Duration.ZERO, List.of(), Map.of()));
            } else if (!probe.supports(context)) {
                results.add(new ProbeResult(probe.id(), probe.safety(), ProbeStatus.SKIPPED, "Required probe inputs are unavailable", Instant.now(), Duration.ZERO, List.of(), Map.of()));
            } else {
                try { results.add(probe.execute(context)); }
                catch (RuntimeException failure) { results.add(new ProbeResult(probe.id(), probe.safety(), ProbeStatus.FAILED, "Probe failed without usable evidence", Instant.now(), Duration.ZERO, List.of(), Map.of("failureType", failure.getClass().getSimpleName()))); }
            }
        });
        return List.copyOf(results);
    }
}
