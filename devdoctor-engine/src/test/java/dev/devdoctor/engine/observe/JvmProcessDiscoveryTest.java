package dev.devdoctor.engine.observe;

import static org.assertj.core.api.Assertions.assertThat;

import dev.devdoctor.engine.security.SecretRedactor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JvmProcessDiscoveryTest {
    @Test void selectsUniqueJvmMatchingProjectArtifact(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>payments-api</artifactId></project>");
        var discovery = new JvmProcessDiscovery(new SecretRedactor()).parse("""
                100 org.gradle.launcher.daemon.bootstrap.GradleDaemon
                200 /apps/payments-api.jar
                300 com.example.UnrelatedApplication
                """, root);

        assertThat(discovery.selected()).get().extracting(JvmProcessDiscovery.JvmProcess::pid).isEqualTo(200L);
        assertThat(discovery.candidates()).hasSize(3);
    }

    @Test void refusesToGuessBetweenAmbiguousJvms(@TempDir Path root) {
        var discovery = new JvmProcessDiscovery(new SecretRedactor()).parse("""
                200 com.example.FirstApplication
                300 com.example.SecondApplication
                """, root);

        assertThat(discovery.selected()).isEmpty();
        assertThat(discovery.message()).contains("Multiple local JVMs", "--pid");
    }
}
