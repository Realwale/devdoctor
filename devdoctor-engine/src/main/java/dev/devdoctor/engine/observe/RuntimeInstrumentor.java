package dev.devdoctor.engine.observe;

import com.sun.tools.attach.VirtualMachine;
import dev.devdoctor.engine.security.SecretRedactor;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Locates and dynamically attaches DevDoctor's outcome-correlation agent to an accessible local JVM. */
public final class RuntimeInstrumentor {
    private final SecretRedactor redactor;

    public RuntimeInstrumentor(SecretRedactor redactor) { this.redactor = redactor; }

    public StartResult startRecording(long processId, Duration duration, Path destination) {
        Path control = locateControlAgent();
        if (control == null) return new StartResult("control-agent-unavailable", false);
        VirtualMachine machine = null;
        try {
            machine = VirtualMachine.attach(Long.toString(processId));
            boolean outcomeAgentActive = Boolean.parseBoolean(
                    machine.getSystemProperties().getProperty("devdoctor.agent.active", "false"));
            String path = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    destination.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
            machine.loadAgent(control.toString(), Math.max(1_000, duration.toMillis()) + ":" + path);
            return new StartResult(outcomeAgentActive ? "outcome-agent-active" : "generic-jfr", true);
        } catch (Exception failure) {
            String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            return new StartResult("agent-attach-failed: " + redactor.redact(message), false);
        } finally {
            if (machine != null) {
                try { machine.detach(); } catch (Exception ignored) { }
            }
        }
    }

    public Path locateAgent() {
        List<Path> candidates = new ArrayList<>();
        add(candidates, System.getProperty("devdoctor.agent.path"));
        add(candidates, System.getenv("DEVDOCTOR_AGENT_JAR"));
        try {
            URI location = RuntimeInstrumentor.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path code = Path.of(location).toAbsolutePath().normalize();
            Path directory = Files.isDirectory(code) ? code : code.getParent();
            if (directory != null) candidates.add(directory.resolve("devdoctor-agent.jar"));
        } catch (Exception ignored) { }
        candidates.add(Path.of("devdoctor-agent-extension", "target", "devdoctor-agent.jar").toAbsolutePath());
        candidates.add(Path.of("..", "devdoctor-agent-extension", "target", "devdoctor-agent.jar").toAbsolutePath());
        return candidates.stream().map(Path::toAbsolutePath).map(Path::normalize).filter(Files::isRegularFile)
                .findFirst().orElse(null);
    }

    Path locateControlAgent() {
        List<Path> candidates = new ArrayList<>();
        add(candidates, System.getProperty("devdoctor.control.agent.path"));
        add(candidates, System.getenv("DEVDOCTOR_CONTROL_AGENT_JAR"));
        try {
            URI location = RuntimeInstrumentor.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path code = Path.of(location).toAbsolutePath().normalize();
            Path directory = Files.isDirectory(code) ? code : code.getParent();
            if (directory != null) candidates.add(directory.resolve("devdoctor-control-agent.jar"));
        } catch (Exception ignored) { }
        Path target = Path.of("devdoctor-agent-extension", "target").toAbsolutePath().normalize();
        addExtensionJar(candidates, target);
        Path siblingTarget = Path.of("..", "devdoctor-agent-extension", "target").toAbsolutePath().normalize();
        addExtensionJar(candidates, siblingTarget);
        return candidates.stream().map(Path::toAbsolutePath).map(Path::normalize).filter(Files::isRegularFile)
                .findFirst().orElse(null);
    }

    private void add(List<Path> candidates, String configured) {
        if (configured != null && !configured.isBlank()) candidates.add(Path.of(configured));
    }

    private void addExtensionJar(List<Path> candidates, Path directory) {
        if (!Files.isDirectory(directory)) return;
        try (var files = Files.list(directory)) {
            files.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString();
                return name.startsWith("devdoctor-agent-extension-") && name.endsWith(".jar")
                        && !name.endsWith("-sources.jar") && !name.endsWith("-javadoc.jar");
            }).findFirst().ifPresent(candidates::add);
        } catch (Exception ignored) { }
    }

    public record StartResult(String status, boolean recordingStarted) { }
}
