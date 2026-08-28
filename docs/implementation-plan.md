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

Phases 0–9 and 11 are implemented in the current 0.1.1 build. Phase 10 remains intentionally optional: the sanitized `HypothesisReasoner` SPI exists, but no external provider is bundled. `clean verify` includes unit, adversarial redaction, command-boundary, automatic-build, healthy false-positive, and all 18 fixture-manifest checks. The CLI benchmark independently reports top-1/top-3 accuracy, false positives, duration, probe counts, and leakage. See [explicit V1 limitations](not-implemented.md) for protocol-level work that is not claimed complete.
