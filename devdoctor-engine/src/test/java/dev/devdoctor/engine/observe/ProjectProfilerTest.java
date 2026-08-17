package dev.devdoctor.engine.observe;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectProfilerTest {
    @Test void identifiesSpringMavenStack(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><properties><java.version>21</java.version><spring-boot.version>3.3.8</spring-boot.version></properties><dependencies><dependency><artifactId>spring-boot-starter-webflux</artifactId></dependency><dependency><artifactId>postgresql</artifactId></dependency></dependencies></project>");
        var profile = new ProjectProfiler().profile(root);
        assertThat(profile.buildTool()).isEqualTo("MAVEN"); assertThat(profile.framework()).isEqualTo("SPRING_BOOT");
        assertThat(profile.webStack()).isEqualTo("WEBFLUX"); assertThat(profile.technologies()).contains("POSTGRESQL");
    }
}
