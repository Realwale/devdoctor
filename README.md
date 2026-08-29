# DevDoctor

**Evidence-based root-cause diagnostics for Java/Spring software failures.**

DevDoctor observes the application. It does not generate, proxy, or replay your HTTP requests. A request may come from Postman, a browser, another service, a load test, or real traffic; the diagnostic model is the same whether there is one request or thousands.

## Runtime diagnosis

For an application that is already running on this Mac:

```bash
devdoctor diagnose
```

DevDoctor selects an unambiguous project JVM, records a bounded JFR window, and correlates runtime exceptions with project, configuration, source, Git, and safe probe evidence. Select a JVM explicitly when necessary:

```bash
jcmd -l
devdoctor diagnose --pid 12345 --observe-seconds 20
```

Thrown exceptions alone are reported as candidates because an application may catch them intentionally. For transaction outcome correlation, start the application with DevDoctor instrumentation:

```bash
devdoctor run -- ./mvnw spring-boot:run
```

Keep that terminal running. Exercise the application normally from anywhere, then use `devdoctor diagnose` in another terminal. Instrumentation records method, normalized route, status, trace linkage, duration, and exception metadata. It never records request/response bodies, header values, cookies, or query values, and it exports no telemetry.

Other evidence sources remain available:

```bash
devdoctor diagnose --log application.log
devdoctor diagnose --command "./mvnw test"
devdoctor diagnose --recording server.jfr
devdoctor diagnose --json
```

`--command` is always explicit. Bare `devdoctor diagnose` never runs Maven or Gradle.

## Evidence access boundary

Installation gives DevDoctor access only to evidence this Mac is authorized to read. It cannot silently inspect a live server on another machine. For a remote server, run DevDoctor there or securely transfer an authorized JFR/log artifact and use `--recording` or `--log`. A missing evidence path produces uncertainty, not a fabricated diagnosis.

DevDoctor is an offline-first diagnostic engine—not a coding agent, request client, repair tool, reverse proxy, or monitoring SaaS. Java 21 is required to build/run the CLI; analyzed applications may use Java 17+.

## Install with Homebrew

```bash
brew tap Realwale/devdoctor https://github.com/Realwale/devdoctor
brew install Realwale/devdoctor/devdoctor
devdoctor version
```

The Intel macOS package contains its own minimized Java runtime and diagnostic agents. Upgrade with `brew upgrade Realwale/devdoctor/devdoctor`.

Build locally with:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./packaging/build-homebrew-release.sh
```

See the [product specification](docs/product-spec.md), [architecture](docs/architecture.md), [privacy model](docs/privacy.md), and [explicit limitations](docs/not-implemented.md).
