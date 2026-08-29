package dev.devdoctor.cli;

import dev.devdoctor.core.json.DiagnosticJson;
import dev.devdoctor.core.model.ProbeSafety;
import dev.devdoctor.engine.DiagnosticEngine;
import dev.devdoctor.engine.DiagnosticRequest;
import dev.devdoctor.engine.HttpRequestSpec;
import dev.devdoctor.engine.report.TerminalReport;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "diagnose", description = "Investigate a software failure.", mixinStandardHelpOptions = true)
final class DiagnoseCommand implements Callable<Integer> {
    @Option(names = "--command", description = "Explicit command to execute and diagnose.") String command;
    @Option(names = "--log", description = "Log file to inspect.") Path log;
    @Option(names = {"--url", "--http"}, description = "HTTP URL to request and diagnose.") URI httpUrl;
    @Option(names = "--method", defaultValue = "GET", description = "HTTP method used with --url.") String httpMethod;
    @Option(names = "--header", description = "HTTP header in 'Name: value' form; repeat for multiple headers.") List<String> httpHeaders = new ArrayList<>();
    @Option(names = "--header-env", description = "HTTP header in 'Name=ENV_VAR' form; reads its value from the environment.") List<String> httpHeaderEnvironments = new ArrayList<>();
    @Option(names = {"--data", "--body"}, description = "Inline HTTP request body.") String httpBody;
    @Option(names = "--data-file", description = "File containing the HTTP request body.") Path httpBodyFile;
    @Option(names = "--expect-status", defaultValue = "200-299", description = "Expected HTTP status or range.") String expectedStatus;
    @Option(names = "--offline", description = "Disable optional external reasoning (deterministic engine remains active).") boolean offline;
    @Option(names = "--json", description = "Emit versioned machine-readable JSON.") boolean json;
    @Option(names = "--verbose", description = "Include probe and graph details.") boolean verbose;
    @Option(names = "--no-save", description = "Do not save the sanitized diagnostic session.") boolean noSave;
    @Option(names = "--no-auto-command", description = "Do not run detected Maven/Gradle tests when no command or log is supplied.") boolean noAutoCommand;
    @Option(names = "--timeout", defaultValue = "120", description = "Command or HTTP request timeout in seconds.") int timeoutSeconds;
    @Option(names = "--output-limit", defaultValue = "1000000", description = "Maximum captured bytes per input stream.") int outputLimit;
    @Option(names = "--project", defaultValue = ".", description = "Project root.") Path project;

    public Integer call() {
        try {
            Path root = project.toAbsolutePath().normalize();
            String logText = readBounded(log, outputLimit);
            HttpRequestSpec httpRequest = httpRequest();
            if (httpRequest != null && !List.of("GET", "HEAD", "OPTIONS").contains(httpRequest.method())) {
                System.err.println("DevDoctor: replaying explicit " + httpRequest.method()
                        + " request; this may mutate the target application.");
            }
            String effectiveCommand = command;
            boolean automaticCommand = false;
            if ((effectiveCommand == null || effectiveCommand.isBlank()) && log == null && httpRequest == null && !noAutoCommand) {
                var automatic = new AutomaticCommandResolver().resolve(root);
                if (automatic.isPresent()) {
                    effectiveCommand = automatic.get().command();
                    automaticCommand = true;
                    System.err.println("DevDoctor: no command or log supplied; running detected "
                            + automatic.get().description() + ": " + effectiveCommand);
                }
            }
            var request = new DiagnosticRequest(root, effectiveCommand, logText, System.getenv(), offline,
                    ProbeSafety.SAFE_ACTIVE, Duration.ofSeconds(Math.max(1, timeoutSeconds)), outputLimit,
                    automaticCommand, httpRequest);
            var session = new DiagnosticEngine().diagnose(request);
            if (json) System.out.println(new DiagnosticJson().write(session)); else new TerminalReport().write(session, new PrintWriter(System.out), verbose);
            if (!noSave) new SessionStore().save(root, session);
            return session.failure().summary().equals("NO FAILURE REPRODUCED") || !session.rootCauses().isEmpty() ? 0 : 2;
        } catch (Exception failure) { System.err.println("DevDoctor could not complete diagnosis: " + failure.getMessage()); return 1; }
    }

    private String readBounded(Path path, int limit) throws IOException {
        if (path == null) return ""; if (!Files.isRegularFile(path)) throw new IOException("Log file not found: " + path);
        try (var input = Files.newInputStream(path)) { return new String(input.readNBytes(Math.max(1, limit)), StandardCharsets.UTF_8); }
    }

    private HttpRequestSpec httpRequest() throws IOException {
        boolean hasHttpOptions = !httpHeaders.isEmpty() || !httpHeaderEnvironments.isEmpty() || httpBody != null || httpBodyFile != null
                || !"GET".equalsIgnoreCase(httpMethod) || !"200-299".equals(expectedStatus);
        if (httpUrl == null) {
            if (hasHttpOptions) {
                throw new IllegalArgumentException("--method, --header, --header-env, --data, --data-file and --expect-status require --url");
            }
            return null;
        }
        if (httpBody != null && httpBodyFile != null) throw new IllegalArgumentException("Use either --data or --data-file, not both");
        String body = httpBody == null ? "" : httpBody;
        if (httpBodyFile != null) {
            if (!Files.isRegularFile(httpBodyFile)) throw new IOException("HTTP body file not found: " + httpBodyFile);
            if (Files.size(httpBodyFile) > outputLimit) throw new IOException("HTTP body file exceeds --output-limit");
            body = Files.readString(httpBodyFile, StandardCharsets.UTF_8);
        }
        if (body.getBytes(StandardCharsets.UTF_8).length > outputLimit) {
            throw new IllegalArgumentException("HTTP body exceeds --output-limit");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        for (String header : httpHeaders) {
            int colon = header == null ? -1 : header.indexOf(':');
            if (colon <= 0) throw new IllegalArgumentException("HTTP headers must use 'Name: value' form");
            headers.put(header.substring(0, colon).trim(), header.substring(colon + 1).stripLeading());
        }
        for (String header : httpHeaderEnvironments) {
            int equals = header == null ? -1 : header.indexOf('=');
            if (equals <= 0 || equals == header.length() - 1) {
                throw new IllegalArgumentException("Environment-backed HTTP headers must use 'Name=ENV_VAR' form");
            }
            String name = header.substring(0, equals).trim();
            String variable = header.substring(equals + 1).trim();
            if (!variable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("Invalid environment variable name for HTTP header: " + variable);
            }
            String value = System.getenv(variable);
            if (value == null) throw new IllegalArgumentException("Environment variable is not set: " + variable);
            headers.put(name, value);
        }
        int[] statuses = parseExpectedStatus(expectedStatus);
        return new HttpRequestSpec(httpUrl, httpMethod, headers, body, statuses[0], statuses[1]);
    }

    private int[] parseExpectedStatus(String value) {
        if (value == null || !value.matches("\\d{3}(?:-\\d{3})?")) {
            throw new IllegalArgumentException("--expect-status must be a status or range such as 200 or 200-299");
        }
        String[] parts = value.split("-", 2);
        int minimum = Integer.parseInt(parts[0]);
        int maximum = parts.length == 1 ? minimum : Integer.parseInt(parts[1]);
        return new int[]{minimum, maximum};
    }
}
