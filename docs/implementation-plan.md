# Implementation plan and status

1. **Architecture:** specifications, graph/domain contract, threat model, ADRs, schema, scenarios, fixtures, benchmark method.
2. **Foundation:** Java 21 Maven modules, Picocli, session/domain/graph, JSON.
3. **Observation:** project, command, stack trace, log, source, config, environment, Git, dependencies.
4. **Security gate:** redaction boundary and adversarial whole-output tests.
5. **Reasoning:** deterministic classification, rule engine, lifecycle, planner, safe probes.
6. **Correlation:** support/contradiction, elimination, confidence, ranking, uncertainty.
7. **Knowledge:** at least 25 Java/Spring scenarios represented as focused rules/signatures.
8. **Fixture lab:** broken and healthy project manifests with acceptance coverage.
9. **Benchmark:** reproducible measurements and leakage gate.
10. **Polish:** terminal/JSON UX, performance, packaging, documentation, acceptance runs.

Every milestone is gated by `./mvnw clean verify`. Critical gaps are recorded as not implemented with impact and next action rather than hidden behind TODOs.

## MVP status

Phases 0–9 and the current polish gate are implemented. Runtime diagnosis includes local JVM discovery, bounded generic JFR, imported recordings, and opt-in outcome-aware Spring HTTP correlation from external traffic. `clean verify` covers caught-exception false positives, one-versus-many event aggregation, an instrumented Spring server receiving traffic from another process, command boundaries, redaction, healthy fixtures, and the fixture benchmark. External AI remains optional and unbundled. See [explicit limitations](not-implemented.md).
