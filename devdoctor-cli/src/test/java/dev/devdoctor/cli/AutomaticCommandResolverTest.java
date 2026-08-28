package dev.devdoctor.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutomaticCommandResolverTest {
    @Test void selectsMavenWrapperForMavenProject(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Path wrapper = Files.writeString(root.resolve("mvnw"), "#!/bin/sh\n");
        wrapper.toFile().setExecutable(true);

        var resolution = new AutomaticCommandResolver().resolve(root).orElseThrow();

        assertThat(resolution.command()).isEqualTo("./mvnw test");
        assertThat(resolution.description()).isEqualTo("Maven test command");
    }

    @Test void selectsGradleWrapperForGradleProject(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("build.gradle.kts"), "plugins { java }");
        Path wrapper = Files.writeString(root.resolve("gradlew"), "#!/bin/sh\n");
        wrapper.toFile().setExecutable(true);

        assertThat(new AutomaticCommandResolver().resolve(root).orElseThrow().command())
                .isEqualTo("./gradlew test");
    }

    @Test void returnsEmptyWhenNoSupportedBuildIsPresent(@TempDir Path root) {
        assertThat(new AutomaticCommandResolver().resolve(root)).isEmpty();
    }
}
