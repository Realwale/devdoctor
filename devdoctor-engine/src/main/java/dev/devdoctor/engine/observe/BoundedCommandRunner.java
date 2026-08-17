package dev.devdoctor.engine.observe;

import dev.devdoctor.engine.security.SecretRedactor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class BoundedCommandRunner {
    private final SecretRedactor redactor;
    private final Duration timeout;
    private final int outputLimit;

    public BoundedCommandRunner(SecretRedactor redactor, Duration timeout, int outputLimit) {
        this.redactor = redactor;
        this.timeout = timeout;
        this.outputLimit = outputLimit;
    }

    /** Executes only the command explicitly supplied by the caller; diagnostic/AI output never reaches this method. */
    public CommandCapture run(Path directory, String command) {
        if (command == null || command.isBlank()) throw new IllegalArgumentException("command must not be blank");
        Instant started = Instant.now();
        Process process;
        try { process = new ProcessBuilder("/bin/sh", "-c", command).directory(directory.toFile()).start(); }
        catch (IOException e) { return new CommandCapture(command, -1, false, false, Duration.between(started, Instant.now()), "", redactor.redact(e.getMessage())); }
        CompletableFuture<Captured> stdout = CompletableFuture.supplyAsync(() -> capture(process.getInputStream()));
        CompletableFuture<Captured> stderr = CompletableFuture.supplyAsync(() -> capture(process.getErrorStream()));
        boolean completed;
        try { completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); completed = false; }
        if (!completed) {
            process.descendants().forEach(ProcessHandle::destroy);
            process.destroy();
            try { if (!process.waitFor(500, TimeUnit.MILLISECONDS)) process.destroyForcibly(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        Captured out = stdout.join(); Captured err = stderr.join();
        int exit = completed ? process.exitValue() : -1;
        return new CommandCapture(command, exit, !completed, out.truncated || err.truncated,
                Duration.between(started, Instant.now()), redactor.redact(out.text), redactor.redact(err.text));
    }

    private Captured capture(InputStream stream) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(outputLimit, 8192));
        byte[] buffer = new byte[4096]; int total = 0; boolean truncated = false;
        try {
            for (int read; (read = stream.read(buffer)) >= 0;) {
                int accepted = Math.min(read, Math.max(0, outputLimit - total));
                if (accepted > 0) output.write(buffer, 0, accepted);
                total += read;
                if (total > outputLimit) truncated = true;
            }
        } catch (IOException ignored) { }
        return new Captured(output.toString(StandardCharsets.UTF_8), truncated);
    }
    private record Captured(String text, boolean truncated) {}
}
