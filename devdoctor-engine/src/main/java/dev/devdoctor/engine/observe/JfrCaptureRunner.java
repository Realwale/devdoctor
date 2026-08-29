package dev.devdoctor.engine.observe;

import dev.devdoctor.engine.security.SecretRedactor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Starts a narrowly scoped JFR recording on a local JVM and deletes raw recording data after analysis. */
public final class JfrCaptureRunner {
    private static final String SETTINGS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <configuration version="2.0" label="DevDoctor Runtime Exceptions"
                           description="Bounded exception evidence only" provider="DevDoctor">
              <event name="jdk.JavaExceptionThrow">
                <setting name="enabled">true</setting>
                <setting name="stackTrace">true</setting>
              </event>
              <event name="jdk.JavaErrorThrow">
                <setting name="enabled">true</setting>
                <setting name="stackTrace">true</setting>
              </event>
              <event name="dev.devdoctor.Transaction">
                <setting name="enabled">true</setting>
                <setting name="stackTrace">false</setting>
                <setting name="threshold">0 ns</setting>
              </event>
            </configuration>
            """;

    private final SecretRedactor redactor;
    private final int outputLimit;
    private final int maximumGroups;

    public JfrCaptureRunner(SecretRedactor redactor, int outputLimit, int maximumGroups) {
        this.redactor = redactor;
        this.outputLimit = Math.max(1_024, outputLimit);
        this.maximumGroups = Math.max(1, maximumGroups);
    }

    public JfrRuntimeObservation capture(long processId, Duration duration) {
        if (processId <= 0 || processId == ProcessHandle.current().pid()) {
            return failed("local-jvm", processId, "A different positive JVM process ID is required");
        }
        Duration boundedDuration = duration == null || duration.isNegative() || duration.isZero()
                ? Duration.ofSeconds(10) : duration.compareTo(Duration.ofHours(1)) > 0 ? Duration.ofHours(1) : duration;
        Path settings = null;
        Path runtimeDirectory = null;
        Path recording = null;
        try {
            settings = secureTempFile("devdoctor-jfr-settings-", ".jfc");
            runtimeDirectory = secureTempDirectory();
            recording = runtimeDirectory.resolve("runtime.jfr");
            Files.writeString(settings, SETTINGS, StandardCharsets.UTF_8);
            RuntimeInstrumentor.StartResult instrumentation = new RuntimeInstrumentor(redactor)
                    .startRecording(processId, boundedDuration, recording);
            if (instrumentation.recordingStarted()) {
                if (!waitForRecording(recording, boundedDuration)) {
                    return failed("local-jvm", processId,
                            "The instrumented JFR recording was not produced before the observation deadline");
                }
                return analyzeWithStatus(recording, processId, instrumentation.status());
            }
            String recordingName = "devdoctor_" + UUID.randomUUID().toString().replace("-", "");
            List<String> command = List.of(jcmdExecutable(), Long.toString(processId), "JFR.start",
                    "name=" + recordingName, "settings=" + settings.toAbsolutePath(),
                    "duration=" + boundedDuration.toSeconds() + "s", "filename=" + recording.toAbsolutePath(),
                    "dumponexit=true", "maxsize=64m");
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean exited = process.waitFor(15, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readNBytes(outputLimit), StandardCharsets.UTF_8);
            if (!exited) {
                process.destroyForcibly();
                return failed("local-jvm", processId, "jcmd did not start the recording within 15 seconds");
            }
            if (process.exitValue() != 0) {
                return failed("local-jvm", processId, "jcmd could not start JFR: " + redactor.redact(output));
            }
            if (!waitForRecording(recording, boundedDuration)) {
                return failed("local-jvm", processId, "JFR recording was not produced before the observation deadline");
            }
            return analyzeWithStatus(recording, processId, instrumentation.status());
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            return failed("local-jvm", processId, "JFR observation was interrupted");
        } catch (Exception failure) {
            return failed("local-jvm", processId, redactor.redact(failure.getMessage() == null
                    ? failure.getClass().getSimpleName() : failure.getMessage()));
        } finally {
            delete(settings);
            delete(recording);
            delete(runtimeDirectory);
        }
    }

    public JfrRuntimeObservation analyze(Path recording) {
        if (recording == null || !Files.isRegularFile(recording)) {
            return failed("imported-jfr", 0, "JFR recording file was not found");
        }
        try {
            return new JfrRuntimeAnalyzer(redactor, maximumGroups, 64)
                    .analyze(recording, "imported-jfr", 0);
        } catch (Exception failure) {
            return failed("imported-jfr", 0, redactor.redact(failure.getMessage() == null
                    ? failure.getClass().getSimpleName() : failure.getMessage()));
        }
    }

    private String jcmdExecutable() {
        Path bundled = Path.of(System.getProperty("java.home"), "bin", "jcmd");
        return Files.isExecutable(bundled) ? bundled.toString() : "jcmd";
    }

    private boolean waitForRecording(Path recording, Duration duration) throws Exception {
        long waitMillis = Math.min(Duration.ofHours(1).toMillis(), duration.plusSeconds(10).toMillis());
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMillis);
        while ((!Files.isRegularFile(recording) || Files.size(recording) == 0) && System.nanoTime() < deadline) {
            Thread.sleep(200);
        }
        return Files.isRegularFile(recording) && Files.size(recording) > 0;
    }

    private JfrRuntimeObservation analyzeWithStatus(Path recording, long processId, String status) throws IOException {
        JfrRuntimeObservation analyzed = new JfrRuntimeAnalyzer(redactor, maximumGroups, 64)
                .analyze(recording, "local-jvm", processId);
        return new JfrRuntimeObservation(analyzed.source(), analyzed.processId(), analyzed.exceptionEvents(),
                analyzed.errorEvents(), analyzed.droppedGroups(), analyzed.duration(), analyzed.exceptionGroups(),
                analyzed.transactionEvents(), analyzed.failedTransactions(), analyzed.transactionGroups(),
                analyzed.hasOutcomeEvidence() ? "outcome-aware" : status, analyzed.captureError());
    }

    private Path secureTempFile(String prefix, String suffix) throws IOException {
        Path file = Files.createTempFile(prefix, suffix);
        try {
            Set<PosixFilePermission> permissions = EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file, permissions);
        } catch (UnsupportedOperationException ignored) { }
        return file;
    }

    private Path secureTempDirectory() throws IOException {
        Path directory = Files.createTempDirectory("devdoctor-runtime-");
        try {
            Files.setPosixFilePermissions(directory, EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        } catch (UnsupportedOperationException ignored) { }
        return directory;
    }

    private JfrRuntimeObservation failed(String source, long processId, String message) {
        return new JfrRuntimeObservation(source, processId, 0, 0, 0, Duration.ZERO, List.of(),
                redactor.redact(message));
    }

    private void delete(Path file) {
        if (file == null) return;
        try { Files.deleteIfExists(file); } catch (IOException ignored) { }
    }
}
