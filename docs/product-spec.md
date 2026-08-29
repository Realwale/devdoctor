# Product specification

## Purpose

DevDoctor answers four auditable questions: what failed, why it failed, what evidence supports the conclusion, and how the conclusion can be verified. The core output is a versioned diagnostic graph consumed by terminal, JSON, CI, IDE, and future MCP transports.

## V1 users and scope

V1 serves Java 17/21 and Spring Boot developers using Maven or Gradle. Supported evidence sources include explicit HTTP request/response reproduction, command output, log files, stack traces, project metadata, configuration shape, environment characteristics, dependencies, Git, processes, ports, DNS, Docker, PostgreSQL/MySQL, Redis, Kafka, and Flyway. Node, Python, Go, .NET, cloud, and Kubernetes diagnostics are plugin expansion points, not V1 implementations.

## User journeys

* `devdoctor diagnose` automatically runs the detected Maven/Gradle test command when no explicit command or log is supplied, then diagnoses the captured exit code and output.
* `devdoctor diagnose --command "./mvnw test"` captures bounded output, exit code, and duration before diagnosis.
* `devdoctor diagnose --log application.log` analyzes a supplied log.
* `devdoctor diagnose --url ... --method POST --header ... --data-file ...` reproduces a Postman-style request, evaluates its expected status, and captures bounded redacted response evidence.
* A passed build/test task is never described as proof of runtime or API health; every no-failure conclusion states its observed scope.
* `--offline` disables optional external reasoning; deterministic diagnosis remains complete.
* `--json` emits schema-versioned, evidence-linked output.
* `--no-auto-command` disables automatic build execution; absence of command/log evidence is reported as unknown, never healthy.
* `evidence E-001` and `hypothesis H-001` explain provenance and reasoning from a locally saved sanitized session.
* `benchmark` evaluates known broken and healthy fixtures without fabricating competitor results.

## Product invariants

1. Evidence has provenance, collection time, strength, and sensitivity.
2. Raw secrets never cross the collection/redaction boundary.
3. A hypothesis is never presented as fact merely because it matches a signature.
4. Correlation is labeled as correlation unless causal evidence exists.
5. Low evidence produces explicit uncertainty, not a guessed root cause.
6. Probes are read-only and safety classified. V1 never repairs, restarts, kills, commits, or mutates infrastructure.
7. Deterministic rules and probes work offline and take precedence over unsupported AI suggestions.
8. A successful observation supports only the behavior actually exercised; build success never implies runtime/API health.

## Success measures

Top-1 and top-3 root-cause accuracy, false-positive rate, evidence completeness, secret leakage, diagnostic duration, probe count, developer interactions, and time to correct diagnosis. Normal passive diagnosis targets under two seconds on a typical project.

## Out of scope

IDE extensions, SaaS, accounts, dashboards, billing, team collaboration, continuous agents, Kubernetes operators, autonomous repair, automatic commits/PRs, chat, vector databases, and multi-language implementations.
