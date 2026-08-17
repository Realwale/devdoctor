package dev.devdoctor.engine.observe;

import java.time.Duration;

public record CommandCapture(String command, int exitCode, boolean timedOut, boolean truncated,
                             Duration duration, String stdout, String stderr) {}
