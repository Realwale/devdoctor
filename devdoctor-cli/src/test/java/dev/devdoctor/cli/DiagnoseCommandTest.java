package dev.devdoctor.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class DiagnoseCommandTest {
    @Test void explicitCommandCanStillBeDiagnosed(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>broken</artifactId><version>1</version></project>");

        Captured result = execute("diagnose", "--project", root.toString(), "--command",
                "printf '[ERROR] COMPILATION ERROR\\n[ERROR] App.java:[7,13] cannot find symbol\\n'; exit 1",
                "--offline", "--no-save");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("Java source compilation failed");
    }

    @Test void diagnosisNeverRunsTheBuildImplicitly(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>must-not-run</artifactId><version>1</version></project>");
        Path marker = root.resolve("build-was-run");
        Path wrapper = Files.writeString(root.resolve("mvnw"), "#!/bin/sh\ntouch '" + marker + "'\n");
        wrapper.toFile().setExecutable(true);

        Captured result = execute("diagnose", "--project", root.toString(), "--offline", "--no-save",
                "--no-auto-runtime");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).contains("NO FAILURE INPUT AVAILABLE");
        assertThat(marker).doesNotExist();
        assertThat(result.stderr()).doesNotContain("Maven", "mvnw", "test command");
    }

    @Test void successfulExplicitCommandIsScopedToOnlyThatEvidence(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>build-only</artifactId><version>1</version></project>");

        Captured result = execute("diagnose", "--project", root.toString(), "--command",
                "printf 'BUILD SUCCESS\\n'", "--offline", "--no-save");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("NO FAILURE REPRODUCED", "applies only to the observed input")
                .doesNotContain("application is healthy");
    }

    @Test void observesRunningJvmWithoutGeneratingOrReplayingRequests(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>runtime-app</artifactId><version>1</version></project>");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process fixture = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                CliRuntimeFailureFixture.class.getName()).redirectErrorStream(true).start();
        try {
            Thread.sleep(500);

            Captured result = execute("diagnose", "--project", root.toString(), "--pid",
                    Long.toString(fixture.pid()), "--observe-seconds", "2", "--offline", "--no-save");

            assertThat(result.exitCode()).isEqualTo(2);
            assertThat(result.stderr()).contains("observing JVM", "does not generate requests");
            assertThat(result.stdout()).contains("RUNTIME EXCEPTION CANDIDATES OBSERVED",
                    "IllegalArgumentException", "Runtime validation failed in application code",
                    "candidates—not confirmed request failures");
        } finally {
            fixture.destroyForcibly();
            fixture.waitFor();
        }
    }

    @Test void runStartsAnExplicitApplicationCommandWithoutOwningItsTraffic(@TempDir Path root) throws Exception {
        Captured result = execute("run", "--project", root.toString(), "--", "/usr/bin/true");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).contains("runtime correlation enabled", "Postman", "another service",
                "devdoctor diagnose");
    }

    @Test void topLevelHelpExposesRuntimeObservationWorkflow() throws Exception {
        Captured result = execute("--help");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("diagnose", "run");
    }

    private Captured execute(String... arguments) throws Exception {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
            System.setOut(out);
            System.setErr(err);
            int exitCode = new CommandLine(new DevDoctorCli()).execute(arguments);
            return new Captured(exitCode, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private record Captured(int exitCode, String stdout, String stderr) { }
}
