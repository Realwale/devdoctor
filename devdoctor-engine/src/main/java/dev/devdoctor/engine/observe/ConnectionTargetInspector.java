package dev.devdoctor.engine.observe;

import dev.devdoctor.core.model.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConnectionTargetInspector {
    private static final Pattern URL = Pattern.compile("(?i)(?:jdbc:)?(?:https?|postgresql|mysql|redis|kafka)://([A-Za-z0-9._-]+)(?::(\\d{2,5}))?");
    private static final Pattern HOST_PORT = Pattern.compile("(?<![\\w.-])([A-Za-z0-9._-]+):(\\d{2,5})(?!\\d)");
    private static final Pattern PORT_WORD = Pattern.compile("(?i)\\bport\\s+(\\d{1,5})\\b");

    public List<Evidence> inspect(String sanitizedText) {
        Set<Target> targets = new LinkedHashSet<>();
        Matcher urls = URL.matcher(sanitizedText);
        while (urls.find()) targets.add(new Target(urls.group(1), urls.group(2) == null ? defaultPort(urls.group()) : Integer.parseInt(urls.group(2))));
        Matcher hostPorts = HOST_PORT.matcher(sanitizedText);
        while (hostPorts.find()) { int port = Integer.parseInt(hostPorts.group(2)); String host = hostPorts.group(1); if (port > 0 && port <= 65535 && !host.chars().allMatch(Character::isDigit)) targets.add(new Target(host, port)); }
        List<Evidence> result = new ArrayList<>(); int id = 1;
        for (Target target : targets.stream().limit(10).toList()) {
            result.add(new Evidence("E-TARGET-" + id++, EvidenceType.CONFIGURATION,
                    new EvidenceSource("FAILURE_TEXT", "connection-target", Map.of()),
                    "Connection target observed: " + target.host + ":" + target.port,
                    EvidenceStrength.MEDIUM, Sensitivity.INTERNAL, Instant.now(), Map.of("host", target.host, "port", target.port)));
        }
        Matcher portWords = PORT_WORD.matcher(sanitizedText);
        while (portWords.find() && id <= 12) {
            int port = Integer.parseInt(portWords.group(1));
            if (port > 0 && port <= 65535 && targets.stream().noneMatch(t -> t.port == port)) {
                result.add(new Evidence("E-TARGET-" + id++, EvidenceType.CONFIGURATION,
                        new EvidenceSource("FAILURE_TEXT", "port", Map.of()), "TCP port observed: " + port,
                        EvidenceStrength.MEDIUM, Sensitivity.INTERNAL, Instant.now(), Map.of("port", port)));
            }
        }
        return List.copyOf(result);
    }
    private int defaultPort(String scheme) {
        String lower = scheme.toLowerCase();
        if (lower.contains("postgresql")) return 5432; if (lower.contains("mysql")) return 3306;
        if (lower.contains("redis")) return 6379; if (lower.contains("kafka")) return 9092; if (lower.contains("https")) return 443; return 80;
    }
    private record Target(String host, int port) {}
}
