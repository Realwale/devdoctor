package dev.devdoctor.cli;

import dev.devdoctor.engine.observe.RuntimeInstrumentor;
import dev.devdoctor.engine.security.SecretRedactor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Starts an application with local, body-free outcome correlation enabled. */
@Command(name = "run", description = "Start an application with DevDoctor runtime correlation.",
        mixinStandardHelpOptions = true)
final class RunCommand implements Callable<Integer> {
    @Option(names = "--project", defaultValue = ".", description = "Application working directory.")
    Path project;

    @Parameters(arity = "1..*", paramLabel = "COMMAND", description = "Application command and arguments after --.")
    List<String> command = new ArrayList<>();

    @Override public Integer call() {
        try {
            Path root = project.toAbsolutePath().normalize();
            Path agent = new RuntimeInstrumentor(new SecretRedactor()).locateAgent();
            if (agent == null) {
                System.err.println("DevDoctor runtime agent was not found. Reinstall DevDoctor or rebuild with ./mvnw verify.");
                return 1;
            }
            ProcessBuilder builder = new ProcessBuilder(command).directory(root.toFile()).inheritIO();
            String existing = builder.environment().getOrDefault("JAVA_TOOL_OPTIONS", "").trim();
            String javaAgent = "-javaagent:\"" + agent.toString().replace("\\", "\\\\")
                    .replace("\"", "\\\"") + "\"";
            String privacyOptions = "-Dotel.traces.exporter=none -Dotel.metrics.exporter=none "
                    + "-Dotel.logs.exporter=none -Dotel.propagators=none";
            String options = javaAgent + " " + privacyOptions;
            builder.environment().put("JAVA_TOOL_OPTIONS", existing.isBlank() ? options : existing + " " + options);
            builder.environment().put("OTEL_TRACES_EXPORTER", "none");
            builder.environment().put("OTEL_METRICS_EXPORTER", "none");
            builder.environment().put("OTEL_LOGS_EXPORTER", "none");
            builder.environment().put("OTEL_PROPAGATORS", "none");
            System.err.println("DevDoctor: runtime correlation enabled for application JVMs.");
            System.err.println("Send traffic normally from Postman, a browser, another service, or production-like load.");
            System.err.println("While the application is running, use `devdoctor diagnose` in another terminal.");
            return builder.start().waitFor();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return 130;
        } catch (Exception failure) {
            System.err.println("DevDoctor could not start the application: " + failure.getMessage());
            return 1;
        }
    }
}
