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
    @Test void bareDiagnoseRunsDetectedBuildAndReportsCompilationFailure(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>broken</artifactId><version>1</version></project>");
        Path wrapper = Files.writeString(root.resolve("mvnw"), """
                #!/bin/sh
                echo '[ERROR] COMPILATION ERROR'
                echo '[ERROR] App.java:[7,13] cannot find symbol'
                exit 1
                """);
        wrapper.toFile().setExecutable(true);

        Captured result = execute("diagnose", "--project", root.toString(), "--offline", "--no-save");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).contains("running detected Maven test command: ./mvnw test");
        assertThat(result.stdout()).contains("Java source compilation failed").doesNotContain("NO FAILURE DETECTED");
    }

    @Test void disablingAutomaticCommandNeverClaimsAnUnobservedProjectIsHealthy(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>unknown</artifactId><version>1</version></project>");

        Captured result = execute("diagnose", "--project", root.toString(), "--offline", "--no-save", "--no-auto-command");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).contains("NO FAILURE INPUT AVAILABLE", "did not run a command").doesNotContain("NO FAILURE DETECTED");
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

    private record Captured(int exitCode, String stdout, String stderr) {}
}
