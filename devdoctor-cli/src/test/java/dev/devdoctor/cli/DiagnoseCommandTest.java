package dev.devdoctor.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
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

    @Test void successfulAutomaticBuildExplicitlyDisclaimsRuntimeCoverage(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>build-only</artifactId><version>1</version></project>");
        Path wrapper = Files.writeString(root.resolve("mvnw"), """
                #!/bin/sh
                echo 'Tests run: 8, Failures: 0, Errors: 0'
                echo 'BUILD SUCCESS'
                exit 0
                """);
        wrapper.toFile().setExecutable(true);

        Captured result = execute("diagnose", "--project", root.toString(), "--offline", "--no-save");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("NO FAILURE REPRODUCED", "BUILD/TEST CHECK PASSED",
                "Runtime and API behavior were not exercised", "not evidence that the application is healthy");
    }

    @Test void replaysPostmanStyleHttpFailureAndDiagnosesResponse(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>runtime-api</artifactId><version>1</version></project>");
        Files.writeString(root.resolve("application.properties"), "runtime.token=${DEVDOCTOR_TEST_RUNTIME_TOKEN_X9}\n");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/orders", exchange -> {
            byte[] response = "Required environment variable DEVDOCTOR_TEST_RUNTIME_TOKEN_X9 is missing"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/orders";

            Captured result = execute("diagnose", "--project", root.toString(), "--url", url,
                    "--method", "POST", "--header", "Authorization: Bearer postman-secret",
                    "--data", "{}", "--offline", "--no-save");

            assertThat(result.exitCode()).isZero();
            assertThat(result.stdout()).contains("HTTP request returned status 500", "Required environment variable is missing")
                    .doesNotContain("NO FAILURE REPRODUCED", "postman-secret");
        } finally {
            server.stop(0);
        }
    }

    @Test void expectedHttpStatusIsScopedToOnlyTheSuppliedRequest(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><artifactId>runtime-api</artifactId><version>1</version></project>");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/missing";

            Captured result = execute("diagnose", "--project", root.toString(), "--url", url,
                    "--expect-status", "404", "--offline", "--no-save");

            assertThat(result.exitCode()).isZero();
            assertThat(result.stdout()).contains("NO FAILURE REPRODUCED", "supplied HTTP request returned an expected status",
                    "applies only to that request", "does not certify the whole application");
        } finally {
            server.stop(0);
        }
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
