package dev.devdoctor.agent;

import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import jdk.jfr.Recording;

/** Attach entry point that starts one bounded recording inside the selected JVM. */
public final class JfrControlAgent {
    private JfrControlAgent() { }

    public static void agentmain(String arguments, Instrumentation ignored) throws Exception {
        String[] parts = arguments == null ? new String[0] : arguments.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid DevDoctor recording arguments");
        long durationMillis = Math.max(1_000, Math.min(Duration.ofHours(1).toMillis(), Long.parseLong(parts[0])));
        String destination = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Recording recording = new Recording();
        recording.setName("devdoctor_" + System.nanoTime());
        recording.setMaxSize(64L * 1_024 * 1_024);
        recording.setDuration(Duration.ofMillis(durationMillis));
        recording.setDestination(Path.of(destination));
        recording.enable("jdk.JavaExceptionThrow").withStackTrace();
        recording.enable("jdk.JavaErrorThrow").withStackTrace();
        recording.enable("dev.devdoctor.Transaction").withoutStackTrace();
        recording.start();
    }
}
