package dev.devdoctor.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Selects the least surprising build verification command for bare {@code diagnose}. */
final class AutomaticCommandResolver {
    Optional<Resolution> resolve(Path projectRoot) {
        Path root = projectRoot.toAbsolutePath().normalize();

        if (Files.isRegularFile(root.resolve("pom.xml"))) {
            Path wrapper = root.resolve("mvnw");
            return Optional.of(new Resolution(
                    Files.isRegularFile(wrapper) ? wrapperCommand(wrapper, "./mvnw test") : "mvn test",
                    "Maven test command"));
        }
        if (Files.isRegularFile(root.resolve("build.gradle"))
                || Files.isRegularFile(root.resolve("build.gradle.kts"))) {
            Path wrapper = root.resolve("gradlew");
            return Optional.of(new Resolution(
                    Files.isRegularFile(wrapper) ? wrapperCommand(wrapper, "./gradlew test") : "gradle test",
                    "Gradle test command"));
        }
        return Optional.empty();
    }

    private String wrapperCommand(Path wrapper, String executableCommand) {
        return Files.isExecutable(wrapper) ? executableCommand : "sh " + executableCommand;
    }

    record Resolution(String command, String description) {}
}
