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
* `devdoctor-engine`: project profiling, parsers, collectors, classifiers, rules, planner, probes, correlation, session orchestration, reporting.
* `devdoctor-cli`: Picocli transport, bounded process execution options, local sanitized session store, benchmark command, executable assembly.

This three-module design keeps transports out of the diagnostic kernel while avoiding premature per-feature modules. Ecosystem plugins implement the SPI and are discovered without modifying correlation logic.

## Dependency direction

`cli -> engine -> core`. Core depends only on Jackson annotations/databind. Engine may depend on OS/JDK APIs and SLF4J. CLI owns Picocli and Logback. No engine class imports CLI types.

## Operational constraints

Collectors skip `.git`, `target`, `build`, `node_modules`, binaries, generated trees, oversized files, and unrelated source. Commands use argument-list execution through the platform shell only when explicitly supplied by the user, with timeout, output byte limits, and descendant cleanup. Probes declare `PASSIVE`, `SAFE_ACTIVE`, `REQUIRES_PERMISSION`, or `PROHIBITED`; the planner selects only allowed probes.

## Extension points

`EvidenceCollector`, `DiagnosticRule`, `DiagnosticProbe`, and `DevDoctorPlugin` are stable SPIs. Future transports call the same `DiagnosticEngine` and serialize the same graph. A graph database is intentionally unnecessary: an in-memory adjacency model persists as JSON.
