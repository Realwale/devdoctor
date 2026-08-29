# Product specification

## Purpose

DevDoctor answers what failed, why it failed, what evidence supports that conclusion, what alternatives were ruled out, and how to verify the result. The core output is a versioned diagnostic graph.

## Runtime invariant

Traffic is external evidence, not a DevDoctor input contract. DevDoctor never requires a user to reproduce a curl command and never assumes a fixed request count. It observes the application JVM and correlates server/dependency outcomes with exceptions, logs, source, configuration, and safe probes. Request origin and concurrency do not change the architecture.

## User journeys

* `devdoctor diagnose` discovers an unambiguous local JVM and captures a bounded JFR observation. It never runs a build implicitly.
* `devdoctor diagnose --pid PID` observes a selected already-running JVM.
* `devdoctor run -- COMMAND...` starts application JVMs with local outcome correlation; normal Postman/browser/service/load traffic remains untouched.
* `devdoctor diagnose --recording server.jfr` analyzes an authorized local or transferred recording.
* `--log`, `--command`, `--offline`, and `--json` provide explicit deterministic evidence paths.
* A generic thrown exception is a candidate. Only an observed failed transaction, failed command, or failure-bearing log confirms a failure.
* Saved evidence and hypotheses remain inspectable by ID.

## Evidence access

DevDoctor can inspect a local accessible JVM and local artifacts. Installation on a laptop does not grant visibility into an unrelated remote server. Remote diagnosis requires DevDoctor on that host or an authorized evidence transfer/telemetry path. The tool states this boundary explicitly.

## Product invariants

1. Evidence has provenance, collection time, strength, and sensitivity.
2. Raw secrets never enter persisted evidence, reports, or external reasoning.
3. No request/response bodies, header values, cookies, or query values are captured by runtime correlation.
4. No telemetry is exported by the bundled runtime agent.
5. A hypothesis is not promoted because it merely resembles an exception signature.
6. Correlation is labeled as correlation unless causal evidence exists.
7. Low evidence produces explicit uncertainty.
8. DevDoctor does not repair, restart, kill, commit, replay traffic, or mutate infrastructure.
9. A no-failure result is scoped to its observation window.

## Scope

V1 focuses on Java 17+ and Spring Boot with Maven/Gradle, MVC/WebFlux, JDBC/JPA, Flyway, PostgreSQL/MySQL, Redis, Kafka, Docker/Compose, and Git evidence. Other languages/cloud platforms remain plugin expansion points.
