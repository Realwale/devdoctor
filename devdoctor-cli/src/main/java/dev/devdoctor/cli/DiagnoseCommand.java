package dev.devdoctor.cli;

import dev.devdoctor.core.json.DiagnosticJson;
import dev.devdoctor.core.model.ProbeSafety;
import dev.devdoctor.engine.DiagnosticEngine;
import dev.devdoctor.engine.DiagnosticRequest;
import dev.devdoctor.engine.report.TerminalReport;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "diagnose", description = "Investigate a software failure.", mixinStandardHelpOptions = true)
final class DiagnoseCommand implements Callable<Integer> {
    @Option(names = "--command", description = "Explicit command to execute and diagnose.") String command;
    @Option(names = "--log", description = "Log file to inspect.") Path log;
    @Option(names = "--offline", description = "Disable optional external reasoning (deterministic engine remains active).") boolean offline;
    @Option(names = "--json", description = "Emit versioned machine-readable JSON.") boolean json;
    @Option(names = "--verbose", description = "Include probe and graph details.") boolean verbose;
    @Option(names = "--no-save", description = "Do not save the sanitized diagnostic session.") boolean noSave;
    @Option(names = "--no-auto-command", description = "Do not run detected Maven/Gradle tests when no command or log is supplied.") boolean noAutoCommand;
    @Option(names = "--timeout", defaultValue = "120", description = "Build command timeout in seconds.") int timeoutSeconds;
    @Option(names = "--output-limit", defaultValue = "1000000", description = "Maximum captured bytes per input stream.") int outputLimit;
    @Option(names = "--project", defaultValue = ".", description = "Project root.") Path project;

    public Integer call() {
        try {
            Path root = project.toAbsolutePath().normalize();
            String logText = readBounded(log, outputLimit);
            String effectiveCommand = command;
            if ((effectiveCommand == null || effectiveCommand.isBlank()) && log == null && !noAutoCommand) {
                var automatic = new AutomaticCommandResolver().resolve(root);
                if (automatic.isPresent()) {
                    effectiveCommand = automatic.get().command();
                    System.err.println("DevDoctor: no command or log supplied; running detected "
                            + automatic.get().description() + ": " + effectiveCommand);
                }
            }
            var request = new DiagnosticRequest(root, effectiveCommand, logText, System.getenv(), offline,
                    ProbeSafety.SAFE_ACTIVE, Duration.ofSeconds(Math.max(1, timeoutSeconds)), outputLimit);
            var session = new DiagnosticEngine().diagnose(request);
            if (json) System.out.println(new DiagnosticJson().write(session)); else new TerminalReport().write(session, new PrintWriter(System.out), verbose);
            if (!noSave) new SessionStore().save(root, session);
            return session.failure().summary().equals("NO FAILURE DETECTED") || !session.rootCauses().isEmpty() ? 0 : 2;
        } catch (Exception failure) { System.err.println("DevDoctor could not complete diagnosis: " + failure.getMessage()); return 1; }
    }

    private String readBounded(Path path, int limit) throws IOException {
        if (path == null) return ""; if (!Files.isRegularFile(path)) throw new IOException("Log file not found: " + path);
        try (var input = Files.newInputStream(path)) { return new String(input.readNBytes(Math.max(1, limit)), StandardCharsets.UTF_8); }
    }
}
