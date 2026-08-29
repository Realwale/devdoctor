package dev.devdoctor.engine.observe;

import dev.devdoctor.engine.security.SecretRedactor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers local application JVMs without selecting an ambiguous process. */
public final class JvmProcessDiscovery {
    private static final Pattern LINE = Pattern.compile("^(\\d+)\\s+(.+)$");
    private static final Pattern ARTIFACT = Pattern.compile("<artifactId>([^<]+)</artifactId>");
    private final SecretRedactor redactor;

    public JvmProcessDiscovery(SecretRedactor redactor) {
        this.redactor = redactor;
    }

    public Discovery discover(Path projectRoot) {
        try {
            Process process = new ProcessBuilder(jcmdExecutable(), "-l").redirectErrorStream(true).start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new Discovery(Optional.empty(), List.of(), "JVM discovery timed out");
            }
            String output = new String(process.getInputStream().readNBytes(200_000), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                return new Discovery(Optional.empty(), List.of(), "JVM discovery failed: " + redactor.redact(output));
            }
            return parse(output, projectRoot);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            return new Discovery(Optional.empty(), List.of(), "JVM discovery was interrupted");
        } catch (IOException failure) {
            return new Discovery(Optional.empty(), List.of(), "jcmd is unavailable; use --recording with a JFR file");
        }
    }

    Discovery parse(String output, Path projectRoot) {
        Set<String> tokens = projectTokens(projectRoot);
        List<JvmProcess> candidates = new ArrayList<>();
        for (String line : output.lines().toList()) {
            Matcher matcher = LINE.matcher(line.trim());
            if (!matcher.matches()) continue;
            long pid = Long.parseLong(matcher.group(1));
            String description = redactor.redact(matcher.group(2).trim());
            String lower = description.toLowerCase(Locale.ROOT);
            if (pid == ProcessHandle.current().pid() || lower.contains("sun.tools.jcmd.jcmd")
                    || lower.contains("jdk.jcmd/sun.tools.jcmd.jcmd")) continue;
            int score = 0;
            for (String token : tokens) if (token.length() >= 3 && lower.contains(token)) score += 100;
            if (lower.contains("spring") || lower.contains("bootrun")) score += 40;
            if (lower.contains(".jar")) score += 20;
            if (lower.contains("gradledaemon") || lower.contains("maven") || lower.contains("surefire")) score -= 100;
            candidates.add(new JvmProcess(pid, description, score));
        }
        candidates.sort(Comparator.comparingInt(JvmProcess::score).reversed().thenComparingLong(JvmProcess::pid));
        Optional<JvmProcess> selected = Optional.empty();
        if (candidates.size() == 1 && candidates.getFirst().score() >= 0) selected = Optional.of(candidates.getFirst());
        else if (!candidates.isEmpty() && candidates.getFirst().score() > 0
                && (candidates.size() == 1 || candidates.getFirst().score() > candidates.get(1).score())) {
            selected = Optional.of(candidates.getFirst());
        }
        String message = selected.isPresent() ? "" : candidates.isEmpty()
                ? "No local application JVM was found"
                : "Multiple local JVMs are plausible; select one with --pid";
        return new Discovery(selected, candidates, message);
    }

    private Set<String> projectTokens(Path root) {
        Set<String> tokens = new LinkedHashSet<>();
        if (root != null && root.getFileName() != null) tokens.add(root.getFileName().toString().toLowerCase(Locale.ROOT));
        Path pom = root == null ? null : root.resolve("pom.xml");
        if (pom != null && Files.isRegularFile(pom)) {
            try {
                Matcher matcher = ARTIFACT.matcher(Files.readString(pom, StandardCharsets.UTF_8));
                while (matcher.find()) tokens.add(matcher.group(1).trim().toLowerCase(Locale.ROOT));
            } catch (IOException ignored) { }
        }
        return tokens;
    }

    private String jcmdExecutable() {
        Path bundled = Path.of(System.getProperty("java.home"), "bin", "jcmd");
        return Files.isExecutable(bundled) ? bundled.toString() : "jcmd";
    }

    public record JvmProcess(long pid, String description, int score) { }
    public record Discovery(Optional<JvmProcess> selected, List<JvmProcess> candidates, String message) {
        public Discovery {
            selected = selected == null ? Optional.empty() : selected;
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
            message = message == null ? "" : message;
        }
    }
}
