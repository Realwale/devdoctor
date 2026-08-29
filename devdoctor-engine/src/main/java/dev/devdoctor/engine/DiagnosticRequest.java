package dev.devdoctor.engine;

import dev.devdoctor.core.model.ProbeSafety;
import dev.devdoctor.engine.observe.JfrRuntimeObservation;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public record DiagnosticRequest(Path projectRoot, String command, String logText, Map<String, String> environment,
                                boolean offline, ProbeSafety maximumSafety, Duration commandTimeout, int outputLimit,
                                JfrRuntimeObservation runtimeObservation) {
    public DiagnosticRequest(Path projectRoot, String command, String logText, Map<String, String> environment,
                             boolean offline, ProbeSafety maximumSafety, Duration commandTimeout, int outputLimit) {
        this(projectRoot, command, logText, environment, offline, maximumSafety, commandTimeout, outputLimit, null);
    }

    public DiagnosticRequest {
        projectRoot = projectRoot == null ? Path.of(".").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
        command = command == null ? "" : command;
        logText = logText == null ? "" : logText;
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        maximumSafety = maximumSafety == null ? ProbeSafety.SAFE_ACTIVE : maximumSafety;
        commandTimeout = commandTimeout == null ? Duration.ofSeconds(30) : commandTimeout;
        outputLimit = outputLimit <= 0 ? 1_000_000 : outputLimit;
    }

    public static DiagnosticRequest passive(Path root, String logText) {
        return new DiagnosticRequest(root, "", logText, System.getenv(), true, ProbeSafety.SAFE_ACTIVE, Duration.ofSeconds(30), 1_000_000);
    }
}
