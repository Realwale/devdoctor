package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.Evidence;
import dev.devdoctor.core.spi.DiagnosticContext;
import java.util.Optional;

final class ProbeSupport {
    private ProbeSupport() {}
    static Optional<String> host(DiagnosticContext context) {
        return context.evidence().stream().map(Evidence::metadata).map(m -> m.get("host")).filter(String.class::isInstance).map(String.class::cast).findFirst();
    }
    static Optional<Integer> port(DiagnosticContext context) {
        return context.evidence().stream().map(Evidence::metadata).map(m -> m.get("port")).filter(Number.class::isInstance).map(Number.class::cast).map(Number::intValue).findFirst();
    }
}
