package dev.devdoctor.cli;

import dev.devdoctor.core.json.DiagnosticJson;
import dev.devdoctor.core.model.ProbeSafety;
import dev.devdoctor.engine.DiagnosticEngine;
import dev.devdoctor.engine.DiagnosticRequest;
import dev.devdoctor.engine.observe.JfrCaptureRunner;
import dev.devdoctor.engine.observe.JfrRuntimeObservation;
import dev.devdoctor.engine.observe.JvmProcessDiscovery;
import dev.devdoctor.engine.report.TerminalReport;
import dev.devdoctor.engine.security.SecretRedactor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "diagnose", description = "Investigate application runtime, logs, recordings, or an explicit command.",
        mixinStandardHelpOptions = true)
final class DiagnoseCommand implements Callable<Integer> {
    @Option(names = "--pid", description = "Local JVM process ID to observe while the application receives normal traffic.") Long processId;
    @Option(names = "--recording", description = "JFR recording captured on this machine or another server.") Path recording;
    @Option(names = "--observe-seconds", defaultValue = "10",
            description = "Seconds to observe the selected local JVM.") int observationSeconds;
    @Option(names = "--command", description = "Explicit command to execute and diagnose.") String command;
    @Option(names = "--log", description = "Application log to inspect.") Path log;
    @Option(names = "--no-auto-runtime", description = "Do not automatically select a matching local JVM.") boolean noAutoRuntime;
    @Option(names = "--offline", description = "Disable optional external reasoning (deterministic engine remains active).") boolean offline;
    @Option(names = "--json", description = "Emit versioned machine-readable JSON.") boolean json;
    @Option(names = "--verbose", description = "Include probe and graph details.") boolean verbose;
    @Option(names = "--no-save", description = "Do not save the sanitized diagnostic session.") boolean noSave;
    @Option(names = "--timeout", defaultValue = "120", description = "Explicit command timeout in seconds.") int timeoutSeconds;
    @Option(names = "--output-limit", defaultValue = "1000000", description = "Maximum captured bytes per input stream.") int outputLimit;
    @Option(names = "--project", defaultValue = ".", description = "Project root used for evidence correlation.") Path project;

    @Override public Integer call() {
        try {
            if (processId != null && recording != null) {
                throw new IllegalArgumentException("Use either --pid or --recording, not both");
            }
            if (processId != null && processId <= 0) throw new IllegalArgumentException("--pid must be positive");
            if (observationSeconds < 1 || observationSeconds > 3_600) {
                throw new IllegalArgumentException("--observe-seconds must be between 1 and 3600");
            }
            if (timeoutSeconds < 1) throw new IllegalArgumentException("--timeout must be positive");
            if (outputLimit < 1) throw new IllegalArgumentException("--output-limit must be positive");

            Path root = project.toAbsolutePath().normalize();
            String logText = readBounded(log, outputLimit);
            SecretRedactor redactor = new SecretRedactor();
            JfrCaptureRunner runtime = new JfrCaptureRunner(redactor, outputLimit, 250);
            JfrRuntimeObservation observation = null;
            if (recording != null) {
                System.err.println("DevDoctor: analyzing runtime recording " + recording.toAbsolutePath().normalize());
                observation = runtime.analyze(recording.toAbsolutePath().normalize());
            } else {
                Long selectedPid = processId;
                boolean noExplicitEvidence = (command == null || command.isBlank()) && log == null;
                if (selectedPid == null && noExplicitEvidence && !noAutoRuntime) {
                    var discovery = new JvmProcessDiscovery(redactor).discover(root);
                    if (discovery.selected().isPresent()) {
                        var selected = discovery.selected().orElseThrow();
                        selectedPid = selected.pid();
                        System.err.println("DevDoctor: selected local JVM " + selected.pid() + " ("
                                + selected.description() + ")");
                    } else {
                        System.err.println("DevDoctor: " + discovery.message());
                        discovery.candidates().forEach(candidate -> System.err.println("  --pid " + candidate.pid()
                                + "  " + candidate.description()));
                    }
                }
                if (selectedPid != null) {
                    System.err.println("DevDoctor: observing JVM " + selectedPid + " for " + observationSeconds + " seconds.");
                    System.err.println("Keep using the application normally; DevDoctor does not generate requests.");
                    observation = runtime.capture(selectedPid, Duration.ofSeconds(observationSeconds));
                }
            }

            var request = new DiagnosticRequest(root, command, logText, System.getenv(), offline,
                    ProbeSafety.SAFE_ACTIVE, Duration.ofSeconds(timeoutSeconds), outputLimit, observation);
            var session = new DiagnosticEngine().diagnose(request);
            if (json) System.out.println(new DiagnosticJson().write(session));
            else new TerminalReport().write(session, new PrintWriter(System.out), verbose);
            if (!noSave) new SessionStore().save(root, session);
            return successful(session.failure().summary(), !session.rootCauses().isEmpty()) ? 0 : 2;
        } catch (Exception failure) {
            System.err.println("DevDoctor could not complete diagnosis: " + failure.getMessage());
            return 1;
        }
    }

    private boolean successful(String summary, boolean rootCauseFound) {
        return summary.equals("NO FAILURE REPRODUCED") || rootCauseFound;
    }

    private String readBounded(Path path, int limit) throws IOException {
        if (path == null) return "";
        if (!Files.isRegularFile(path)) throw new IOException("Log file not found: " + path);
        try (var input = Files.newInputStream(path)) {
            return new String(input.readNBytes(limit), StandardCharsets.UTF_8);
        }
    }
}
