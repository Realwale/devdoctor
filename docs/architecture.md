# Architecture

## Pipeline

```text
Failure input -> collectors -> normalized evidence -> redaction boundary
  -> classification -> rules/hypotheses -> planner -> safe probes
  -> support/contradiction correlation -> confidence/ranking
  -> diagnostic graph -> terminal / JSON / local sanitized session
```

No component can bypass the redaction boundary. AI, when later supplied, receives only `SanitizedDiagnosticContext` and may propose untested hypotheses; it cannot create evidence or confirm a conclusion.

## Modules

* `devdoctor-core`: immutable domain records, graph, JSON contracts, redaction primitives, plugin SPI.
* `devdoctor-agent-extension`: optional OpenTelemetry Java-agent extension and bounded JFR control agent. It emits local, body-free transaction outcome events and exports no telemetry.
* `devdoctor-engine`: runtime discovery/JFR analysis, project profiling, parsers, collectors, classifiers, rules, planner, probes, correlation, session orchestration, reporting.
* `devdoctor-cli`: Picocli transport, bounded process execution options, local sanitized session store, benchmark command, executable assembly.

The agent is an evidence collector, not the diagnostic engine. The engine still analyzes logs, commands, imported recordings, and generic JFR without it.

## Dependency direction

`cli -> engine -> core`; the agent module is isolated from all three and compiles for Java 17. Core depends only on Jackson. No engine class imports CLI types.

## Operational constraints

Collectors skip `.git`, `target`, `build`, `node_modules`, binaries, generated trees, oversized files, and unrelated source. Commands use argument-list execution through the platform shell only when explicitly supplied by the user, with timeout, output byte limits, and descendant cleanup. Probes declare `PASSIVE`, `SAFE_ACTIVE`, `REQUIRES_PERMISSION`, or `PROHIBITED`; the planner selects only allowed probes.

Runtime observation has two levels:

* Generic JFR attaches to an accessible local JVM and yields bounded exception candidates without changing request handling.
* Outcome-aware JFR is available when the application was started through `devdoctor run` or an equivalent explicit `-javaagent` configuration. Completed OpenTelemetry spans become custom JFR events correlated by trace ID. The custom sampler records spans only while the bounded DevDoctor JFR window is active. A small attach-time control agent starts that window; DevDoctor never hot-injects the full instrumentation agent into an already-serving JVM.

Imported JFR uses the same analyzer. Remote visibility is an evidence-access concern, not hidden networking: no remote collector or account is bundled.

## Extension points

`EvidenceCollector`, `DiagnosticRule`, `DiagnosticProbe`, and `DevDoctorPlugin` are stable SPIs. Future transports call the same `DiagnosticEngine` and serialize the same graph. A graph database is intentionally unnecessary: an in-memory adjacency model persists as JSON.
