package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClassAvailabilityProbe implements DiagnosticProbe {
    private static final Pattern CLASS = Pattern.compile("(?:ClassNotFoundException|NoClassDefFoundError):?\\s+([A-Za-z_$][\\w$]*(?:[./][A-Za-z_$][\\w$]*)+)");
    public String id() { return "class-availability"; }
    public boolean supports(DiagnosticContext c) { return CLASS.matcher(c.failure().exceptionClass() + ": " + c.failure().message()).find(); }
    public ProbeSafety safety() { return ProbeSafety.PASSIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        Matcher matcher = CLASS.matcher(c.failure().exceptionClass() + ": " + c.failure().message()); matcher.find();
        String name = matcher.group(1).replace('/', '.'); boolean present;
        try { Class.forName(name, false, Thread.currentThread().getContextClassLoader()); present = true; } catch (Throwable ignored) { present = false; }
        Evidence e = new Evidence("E-PROBE-CLASS", EvidenceType.DEPENDENCY, new EvidenceSource("DIAGNOSTIC_CLASSPATH", name, Map.of()), present ? "Class is available to the diagnostic runtime" : "Class is absent from the diagnostic runtime classpath", EvidenceStrength.MEDIUM, Sensitivity.PUBLIC, Instant.now(), Map.of("className", name, "available", present, "scope", "diagnostic runtime, not application runtime"));
        return new ProbeResult(id(), safety(), present ? ProbeStatus.SUCCEEDED : ProbeStatus.FAILED, e.summary(), Instant.now(), Duration.ZERO, List.of(e), Map.of("className", name, "available", present));
    }
}
