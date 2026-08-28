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

When no `--command` or `--log` is supplied, `devdoctor diagnose` now runs the detected Maven or Gradle test command and diagnoses its exit code and bounded output. Use `--no-auto-command` when you only want already-available evidence; DevDoctor will report that no failure input was observed instead of claiming the project is healthy.

DevDoctor observes a failure, gathers sanitized evidence, generates competing hypotheses, runs safe diagnostic probes, rules out explanations, and emits an auditable diagnostic graph. It is an offline-first diagnostic engine—not a coding agent, chatbot, repair tool, or monitoring service.

Java 21 is required to build and run DevDoctor. The analyzed application may target Java 17 or Java 21.

## Install with Homebrew

Build a checksum-pinned local release formula and install it:

```bash
./packaging/build-homebrew-release.sh
brew install --formula ./outputs/devdoctor.rb
devdoctor version
```

This installs a self-contained Intel macOS build into Homebrew and exposes `devdoctor` through `/usr/local/bin`. Upgrade with `brew reinstall --formula ./outputs/devdoctor.rb` and remove it with `brew uninstall devdoctor`.

The Intel macOS package includes a minimized Java 21 runtime, so the installed command does not depend on `JAVA_HOME`, Xcode, or a separate JDK formula. See `packaging/homebrew/README.md` for local verification and public-tap publishing instructions.

The V1 target is Java 17/21 and Spring Boot projects using Maven or Gradle, with focused diagnostics for configuration, dependencies, databases, networking, Docker, Redis, Kafka, Flyway, and JVM failures. Raw secrets are redacted before logging, persistence, reporting, or optional external reasoning.

See [product specification](docs/product-spec.md), [architecture](docs/architecture.md), and [implementation status](docs/implementation-plan.md).
