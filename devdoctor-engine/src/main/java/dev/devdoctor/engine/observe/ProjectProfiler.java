package dev.devdoctor.engine.observe;

import dev.devdoctor.core.model.ProjectProfile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProjectProfiler {
    private static final long MAX_BUILD_FILE = 2_000_000;

    public ProjectProfile profile(Path root) {
        Path normalized = root.toAbsolutePath().normalize();
        boolean maven = Files.isRegularFile(normalized.resolve("pom.xml"));
        boolean gradle = Files.isRegularFile(normalized.resolve("build.gradle")) || Files.isRegularFile(normalized.resolve("build.gradle.kts"));
        String buildTool = maven ? "MAVEN" : gradle ? "GRADLE" : "UNKNOWN";
        String build = readSmall(maven ? normalized.resolve("pom.xml") : gradle ? firstExisting(normalized.resolve("build.gradle"), normalized.resolve("build.gradle.kts")) : null);
        String framework = build.contains("spring-boot") ? "SPRING_BOOT" : "NONE";
        String springVersion = firstMatch(build, "<spring-boot.version>([^<]+)", "org\\.springframework\\.boot[^\\n]*version\\s*[= ]\\s*[\\\"']([^\\\"']+)");
        String javaVersion = firstMatch(build, "<(?:java.version|maven.compiler.release)>([^<]+)", "JavaVersion\\.VERSION_(\\d+)");
        if (javaVersion.isBlank()) javaVersion = runtimeJavaVersion();
        String webStack = build.contains("spring-boot-starter-webflux") ? "WEBFLUX" : build.contains("spring-boot-starter-web") ? "MVC" : "NONE";
        List<String> technologies = new ArrayList<>();
        detect(build, technologies, "flyway", "FLYWAY"); detect(build, technologies, "postgresql", "POSTGRESQL");
        detect(build, technologies, "mysql", "MYSQL"); detect(build, technologies, "redis", "REDIS");
        detect(build, technologies, "kafka", "KAFKA"); detect(build, technologies, "hibernate", "HIBERNATE");
        if (Files.isRegularFile(normalized.resolve("Dockerfile"))) technologies.add("DOCKER");
        if (Files.isRegularFile(normalized.resolve("compose.yml")) || Files.isRegularFile(normalized.resolve("docker-compose.yml"))) technologies.add("DOCKER_COMPOSE");
        if (Files.isDirectory(normalized.resolve(".git"))) technologies.add("GIT");
        Map<String,String> attributes = new LinkedHashMap<>();
        attributes.put("mavenWrapper", Boolean.toString(Files.isRegularFile(normalized.resolve("mvnw"))));
        attributes.put("gradleWrapper", Boolean.toString(Files.isRegularFile(normalized.resolve("gradlew"))));
        return new ProjectProfile(normalized.toString(), buildTool.equals("UNKNOWN") ? "UNKNOWN" : "JAVA", javaVersion,
                buildTool, framework, springVersion.isBlank() ? "UNKNOWN" : springVersion, webStack,
                technologies.stream().distinct().sorted().toList(), attributes);
    }

    private void detect(String text, List<String> values, String needle, String value) { if (text.toLowerCase().contains(needle)) values.add(value); }
    private String runtimeJavaVersion() { return System.getProperty("java.specification.version", "UNKNOWN"); }
    private Path firstExisting(Path first, Path second) { return Files.exists(first) ? first : second; }
    private String readSmall(Path path) {
        if (path == null) return "";
        try { if (!Files.isRegularFile(path) || Files.size(path) > MAX_BUILD_FILE) return ""; return Files.readString(path, StandardCharsets.UTF_8); }
        catch (IOException ignored) { return ""; }
    }
    private String firstMatch(String text, String... expressions) {
        for (String expression : expressions) { Matcher m = Pattern.compile(expression, Pattern.CASE_INSENSITIVE).matcher(text); if (m.find()) return m.group(1).trim(); }
        return "";
    }
}
