package dev.devdoctor.engine;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.DiagnosticContext;
import dev.devdoctor.core.spi.DiagnosticProbe;
import dev.devdoctor.engine.knowledge.*;
import dev.devdoctor.engine.observe.*;
import dev.devdoctor.engine.probe.*;
import dev.devdoctor.engine.reasoning.*;
import dev.devdoctor.engine.security.SecretRedactor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DiagnosticEngine {
    private final SecretRedactor redactor = new SecretRedactor();
    private final JavaSpringKnowledgeBase knowledge = new JavaSpringKnowledgeBase();

    public DiagnosticSession diagnose(DiagnosticRequest request) {
        Instant started = Instant.now(); Path root = request.projectRoot();
        ProjectProfile profile = new ProjectProfiler().profile(root);
        List<Evidence> evidence = new ArrayList<>();
        evidence.add(new Evidence("E-PROJECT", EvidenceType.PROJECT, new EvidenceSource("PROJECT", root.toString(), Map.of()),
                "Project profile collected", EvidenceStrength.HIGH, Sensitivity.INTERNAL, started, Map.of("profile", profile)));

        StringBuilder failureText = new StringBuilder();
        CommandCapture commandCapture = null;
        if (!request.command().isBlank()) {
            commandCapture = new BoundedCommandRunner(redactor, request.commandTimeout(), request.outputLimit()).run(root, request.command());
            failureText.append(commandCapture.stdout()).append('\n').append(commandCapture.stderr());
            evidence.add(new Evidence("E-COMMAND", EvidenceType.COMMAND, new EvidenceSource("COMMAND", "explicit-user-command", Map.of()),
                    "Command exited with code " + commandCapture.exitCode() + (commandCapture.timedOut() ? " after timing out" : ""),
                    commandCapture.exitCode() == 0 ? EvidenceStrength.MEDIUM : EvidenceStrength.HIGH, Sensitivity.INTERNAL, started,
                    Map.of("exitCode", commandCapture.exitCode(), "timedOut", commandCapture.timedOut(), "truncated", commandCapture.truncated(),
                            "durationMillis", commandCapture.duration().toMillis(), "stdout", commandCapture.stdout(), "stderr", commandCapture.stderr())));
        }
        String suppliedLog = request.logText();
        if (suppliedLog.isBlank() && request.command().isBlank()) suppliedLog = readDefaultLog(root);
        if (!suppliedLog.isBlank()) {
            String safeLog = redactor.redact(limit(suppliedLog, request.outputLimit()));
            failureText.append('\n').append(safeLog);
            List<LogEvent> events = new StructuredLogParser(redactor).parse(safeLog);
            evidence.add(new Evidence("E-LOG", EvidenceType.LOG, new EvidenceSource("LOG", request.logText().isBlank() ? "application.log" : "supplied-log", Map.of()),
                    "Log evidence collected: " + events.size() + " structured error/warning events", EvidenceStrength.HIGH, Sensitivity.INTERNAL, started,
                    Map.of("text", safeLog, "events", events)));
        }
        String safeFailureText = redactor.redact(failureText.toString());
        JavaStackTraceParser stackParser = new JavaStackTraceParser(); ParsedStackTrace trace = stackParser.parse(safeFailureText);
        ParsedException rootException = trace.rootCause();
        boolean inputObserved = commandCapture != null || !suppliedLog.isBlank();
        boolean detected = isFailureDetected(commandCapture, suppliedLog);
        String exceptionClass = detected && rootException != null ? rootException.exceptionClass() : "";
        String message = detected ? (rootException == null ? firstFailureLine(safeFailureText) : rootException.message()) : "";
        String failureSummary = !inputObserved ? "NO FAILURE INPUT AVAILABLE"
                : !detected ? "NO FAILURE DETECTED"
                : rootException != null ? summary(exceptionClass, message)
                : commandCapture != null && (commandCapture.exitCode() != 0 || commandCapture.timedOut())
                    ? commandFailureSummary(commandCapture, message)
                    : summary(exceptionClass, message);
        Failure failure = new Failure("F-1", failureSummary, exceptionClass, message, List.of(), started);
        if (rootException != null) evidence.add(new Evidence("E-STACK", EvidenceType.STACK_TRACE, new EvidenceSource("STACK_TRACE", exceptionClass, Map.of()),
                "Root exception: " + exceptionClass + (message.isBlank() ? "" : ": " + message), EvidenceStrength.VERY_HIGH, Sensitivity.INTERNAL, started,
                Map.of("thread", trace.thread(), "causeDepth", trace.causeChain().size(), "applicationFrames", trace.applicationFrames())));

        var config = new ConfigurationInspector(redactor).inspect(root, request.environment()); evidence.addAll(config.evidence());
        evidence.addAll(new SourceInspector(redactor).inspect(root, trace.applicationFrames()));
        evidence.addAll(new ConnectionTargetInspector().inspect(safeFailureText));
        evidence.addAll(new GitInspector(redactor).inspect(root));

        if (!detected) {
            Failure healthy = new Failure(failure.id(), failure.summary(), "", "", List.of(), started);
            DiagnosticGraph graph = new DiagnosticGraphBuilder().build(healthy, evidence, List.of(), List.of(), List.of(), List.of(), List.of());
            return new DiagnosticSession("1.0", newId(), started, healthy, profile, evidence, List.of(), List.of(), List.of(), List.of(), List.of(), graph);
        }

        List<SignatureRuleEngine.MatchedHypothesis> matched = new SignatureRuleEngine().evaluate(knowledge.signatures(), evidence);
        LinkedHashSet<FailureClassification> classifications = matched.stream().flatMap(m -> m.signature().classifications().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
        if (classifications.isEmpty()) classifications.add(FailureClassification.UNKNOWN);
        failure = new Failure(failure.id(), failure.summary(), failure.exceptionClass(), failure.message(), List.copyOf(classifications), failure.observedAt());
        List<Hypothesis> hypotheses = matched.stream().map(SignatureRuleEngine.MatchedHypothesis::hypothesis).toList();
        DiagnosticContext beforeProbes = new DiagnosticContext(root, failure, profile, evidence, request.maximumSafety());
        List<ProbeResult> probes = new ProbePlanner().execute(hypotheses, probes(), beforeProbes);
        probes.forEach(p -> evidence.addAll(p.evidence()));
        CorrelationEngine correlation = new CorrelationEngine(); ConfidenceEngine confidence = new ConfidenceEngine();
        List<Hypothesis> assessed = new ArrayList<>();
        for (SignatureRuleEngine.MatchedHypothesis item : matched) {
            Hypothesis correlated = correlation.correlate(item.hypothesis(), probes);
            assessed.add(confidence.assess(correlated, item.signature(), evidence, probes));
        }
        assessed.sort(Comparator.comparingInt(this::rankingScore).reversed().thenComparing(Hypothesis::id));
        List<Hypothesis> stableIds = reassignIds(assessed);
        Map<String,DiagnosticSignature> signatureByRule = knowledge.signatures().stream().collect(Collectors.toMap(DiagnosticSignature::id, s -> s));
        List<RootCauseCandidate> roots = new ArrayList<>(); int rank = 1;
        for (Hypothesis h : stableIds) {
            if (h.confidence().ordinal() < Confidence.HIGH.ordinal() || h.status() != HypothesisStatus.SUPPORTED) continue;
            boolean subsumed = roots.stream().map(r -> find(stableIds, r.hypothesisId()))
                    .anyMatch(existing -> existing.supportingEvidence().containsAll(h.supportingEvidence()) && rankingScore(existing) > rankingScore(h));
            if (!subsumed) roots.add(new RootCauseCandidate(h.id(), h.title(), h.confidence(), h.supportingEvidence(), rank++));
        }
        List<Remediation> remediations = roots.stream().map(r -> new Remediation("R-" + r.rank(), r.hypothesisId(), signatureByRule.get(find(stableIds, r.hypothesisId()).ruleId()).remediation(), false)).toList();
        List<VerificationStep> verification = roots.stream().map(r -> new VerificationStep("V-" + r.rank(), r.hypothesisId(), signatureByRule.get(find(stableIds, r.hypothesisId()).ruleId()).verification(), "", true)).toList();
        DiagnosticGraph graph = new DiagnosticGraphBuilder().build(failure, evidence, stableIds, probes, roots, remediations, verification);
        return new DiagnosticSession("1.0", newId(), started, failure, profile, evidence, stableIds, probes, roots, remediations, verification, graph);
    }

    private List<DiagnosticProbe> probes() { return List.of(new JavaVersionProbe(), new EnvironmentVariablePresenceProbe(), new EnvironmentWhitespaceProbe(), new ConfigurationValueShapeProbe(), new FileExistenceProbe(), new DnsResolutionProbe(), new TcpConnectivityProbe(), new PortAvailabilityProbe(redactor), new PortOwnerProbe(redactor), new DatabaseReachabilityProbe(), new RedisReachabilityProbe(), new DockerDaemonProbe(redactor), new DockerServiceStateProbe(redactor), new ClassAvailabilityProbe(), new DependencyVersionProbe(), new GitChangeProbe()); }
    private List<Hypothesis> reassignIds(List<Hypothesis> sorted) { List<Hypothesis> result = new ArrayList<>(); int i = 1; for (Hypothesis h : sorted) result.add(new Hypothesis("H-" + String.format(Locale.ROOT, "%03d", i++), h.ruleId(), h.title(), h.description(), h.status(), h.confidence(), h.supportingEvidence(), h.contradictingEvidence(), h.availableProbes(), h.confidenceFactors())); return List.copyOf(result); }
    private Hypothesis find(List<Hypothesis> hypotheses, String id) { return hypotheses.stream().filter(h -> h.id().equals(id)).findFirst().orElseThrow(); }
    private int rankingScore(Hypothesis hypothesis) {
        int score = hypothesis.confidence().ordinal() * 100 + hypothesis.supportingEvidence().size() * 2 - hypothesis.contradictingEvidence().size() * 4;
        if (hypothesis.confidenceFactors().stream().anyMatch(f -> f.startsWith("causal linkage"))) score += 20;
        if (hypothesis.confidenceFactors().stream().anyMatch(f -> f.startsWith("specific multi-signal"))) score += 5;
        return score;
    }
    private String newId() { return "DD-" + java.time.LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT); }
    private boolean isFailureDetected(CommandCapture command, String logText) {
        if (command != null && (command.exitCode() != 0 || command.timedOut())) return true;
        return !logText.isBlank() && logText.matches("(?is).*(exception|error|failed|failure|connection refused|timed out).*" );
    }
    private String summary(String type, String message) { if (!type.isBlank()) return type + (message.isBlank() ? "" : ": " + message); return message.isBlank() ? "Software failure observed" : message; }
    private String commandFailureSummary(CommandCapture command, String message) {
        if (command.timedOut()) return "Command timed out after " + command.duration().toSeconds() + " seconds";
        String detail = message.equals("Software failure observed") ? firstMeaningfulLine(command.stderr(), command.stdout()) : message;
        return "Command failed with exit code " + command.exitCode() + (detail.isBlank() ? "" : ": " + detail);
    }
    private String firstFailureLine(String text) { return text.lines().map(String::trim).filter(s -> !s.isBlank()).filter(s -> s.matches("(?i).*(exception|error|failed|failure|refused|timed out).*" )).findFirst().map(this::limitSummary).orElse("Software failure observed"); }
    private String firstMeaningfulLine(String... texts) {
        for (String text : texts) {
            String line = text.lines().map(String::trim).filter(s -> !s.isBlank()).findFirst().orElse("");
            if (!line.isBlank()) return limitSummary(line);
        }
        return "";
    }
    private String limitSummary(String value) { return value.length() <= 300 ? value : value.substring(0, 297) + "..."; }
    private String readDefaultLog(Path root) { Path log = root.resolve("application.log"); try { return Files.isRegularFile(log) ? Files.readString(log, StandardCharsets.UTF_8) : ""; } catch (IOException ignored) { return ""; } }
    private String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
}
