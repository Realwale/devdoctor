package dev.devdoctor.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.devdoctor.core.model.ProbeSafety;
import dev.devdoctor.engine.DiagnosticEngine;
import dev.devdoctor.engine.DiagnosticRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "benchmark", description = "Run the reproducible fixture benchmark.", mixinStandardHelpOptions = true)
final class BenchmarkCommand implements Callable<Integer> {
    @Option(names = "--fixtures", defaultValue = "test-fixtures") Path fixtures;
    public Integer call() throws Exception {
        ObjectMapper mapper = new ObjectMapper(); List<Path> manifests;
        try (var files = Files.find(fixtures, 3, (p,a) -> a.isRegularFile() && p.getFileName().toString().equals("fixture.json"))) { manifests = files.sorted().toList(); }
        int top1 = 0, top3 = 0, falsePositives = 0, leakage = 0, probes = 0; List<Long> durations = new ArrayList<>(); List<String> mismatches = new ArrayList<>();
        for (Path manifest : manifests) {
            JsonNode spec = mapper.readTree(Files.readString(manifest)); Path root = manifest.getParent(); String log = Files.readString(root.resolve(spec.path("log").asText("failure.log")), StandardCharsets.UTF_8);
            Instant start = Instant.now(); var session = new DiagnosticEngine().diagnose(new DiagnosticRequest(root, "", log, environment(spec), true, ProbeSafety.PASSIVE, Duration.ofSeconds(10), 500_000)); durations.add(Duration.between(start, Instant.now()).toMillis()); probes += session.probes().size();
            String expected = spec.path("expectedRuleId").asText(); boolean healthy = spec.path("healthy").asBoolean(false);
            List<String> rules = session.rootCauses().stream().map(r -> session.hypotheses().stream().filter(h -> h.id().equals(r.hypothesisId())).findFirst().orElseThrow().ruleId()).toList();
            if (!healthy && !rules.isEmpty() && rules.getFirst().equals(expected)) top1++; else if (!healthy) mismatches.add(spec.path("name").asText() + ": expected " + expected + ", got " + rules);
            if (!healthy && rules.stream().limit(3).anyMatch(expected::equals)) top3++; if (healthy && !rules.isEmpty()) { falsePositives++; mismatches.add(spec.path("name").asText() + ": healthy fixture produced " + rules); }
            for (JsonNode canary : spec.path("secretCanaries")) if (new dev.devdoctor.core.json.DiagnosticJson().write(session).contains(canary.asText())) leakage++;
        }
        long average = durations.stream().mapToLong(Long::longValue).sum() / Math.max(1, durations.size()); int broken = (int) manifests.stream().filter(p -> { try { return !mapper.readTree(p.toFile()).path("healthy").asBoolean(false); } catch (Exception e) { return false; } }).count();
        System.out.printf("DEVDOCTOR BENCHMARK%n%nScenarios:              %d%nCorrect Top-1:          %d%nCorrect Top-3:          %d%nTop-1 Accuracy:         %.1f%%%nTop-3 Accuracy:         %.1f%%%nFalse positives:        %d%nAverage diagnosis:      %d ms%nAverage probes:         %.1f%nSecret leakage tests:   %s%n", manifests.size(), top1, top3, broken == 0 ? 100 : top1 * 100.0 / broken, broken == 0 ? 100 : top3 * 100.0 / broken, falsePositives, average, manifests.isEmpty() ? 0 : probes * 1.0 / manifests.size(), leakage == 0 ? "PASS" : "FAIL (" + leakage + ")");
        if (!mismatches.isEmpty()) { System.out.println("\nMismatches:"); mismatches.forEach(m -> System.out.println("- " + m)); }
        return leakage == 0 && falsePositives == 0 && top1 == broken ? 0 : 2;
    }
    private java.util.Map<String,String> environment(JsonNode spec) { java.util.Map<String,String> env = new java.util.HashMap<>(); spec.path("environment").fields().forEachRemaining(e -> env.put(e.getKey(), e.getValue().asText())); return env; }
}
