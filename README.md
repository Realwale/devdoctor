# DevDoctor

**Evidence-based root-cause diagnostics for software failures.**

```bash
export JAVA_HOME=/path/to/jdk-21
./mvnw -q clean verify
./devdoctor diagnose
./devdoctor diagnose --log application.log
./devdoctor diagnose --command "./mvnw test"
./devdoctor diagnose --json
```

When no explicit evidence is supplied, `devdoctor diagnose` runs the detected Maven or Gradle test command. A successful build is reported only as a passed build/test check; DevDoctor explicitly states that runtime and API behavior were not exercised and never equates that result with application health.

## Reproduce a Postman/API failure

Replay the failing request against the running application:

```bash
devdoctor diagnose \
  --url http://localhost:8080/api/orders \
  --method POST \
  --header "Content-Type: application/json" \
  --header-env "Authorization=AUTHORIZATION_HEADER" \
  --data-file request.json \
  --expect-status 200-299
```

Add `--log application.log` to correlate the HTTP response with server-side log evidence in the same session. Set `AUTHORIZATION_HEADER` to the complete header value (for example, `Bearer …`). Header values, request bodies, URL query values, and response bodies are retained only in memory long enough to execute the request; persisted evidence contains header names, a query-free URL, status/timing, and a redacted bounded response. Prefer `--header-env` and `--data-file`; inline `--header`/`--data` values may remain visible in shell history or process arguments.

Only requests explicitly supplied with `--url` are sent. Replaying `POST`, `PUT`, `PATCH`, or `DELETE` can mutate the target system just as it can in Postman; DevDoctor warns before executing those methods. Use `--no-auto-command` to disable the default build/test check when no request, command, or log is supplied.

DevDoctor observes a failure, gathers sanitized evidence, generates competing hypotheses, runs safe diagnostic probes, rules out explanations, and emits an auditable diagnostic graph. It is an offline-first diagnostic engine—not a coding agent, chatbot, repair tool, or monitoring service.

Java 21 is required to build and run DevDoctor. The analyzed application may target Java 17 or Java 21.

## Install with Homebrew

Install the checksum-pinned Intel macOS release from the repository tap:

```bash
brew tap Realwale/devdoctor https://github.com/Realwale/devdoctor
brew install Realwale/devdoctor/devdoctor
devdoctor version
```

This installs a self-contained Intel macOS build into Homebrew and exposes `devdoctor` through `/usr/local/bin`. Upgrade with `brew upgrade Realwale/devdoctor/devdoctor` and remove it with `brew uninstall devdoctor`.

To build and test the formula locally while developing DevDoctor:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./packaging/build-homebrew-release.sh
```

The Intel macOS package includes a minimized Java 21 runtime, so the installed command does not depend on `JAVA_HOME`, Xcode, or a separate JDK formula. See `packaging/homebrew/README.md` for local verification and public-tap publishing instructions.

The V1 target is Java 17/21 and Spring Boot projects using Maven or Gradle, with focused diagnostics for runtime HTTP responses, configuration, dependencies, databases, networking, Docker, Redis, Kafka, Flyway, and JVM failures. Raw secrets are redacted before logging, persistence, reporting, or optional external reasoning.

See [product specification](docs/product-spec.md), [architecture](docs/architecture.md), and [implementation status](docs/implementation-plan.md).
