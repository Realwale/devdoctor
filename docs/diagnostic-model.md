# Diagnostic model

## Core entities

`DiagnosticSession` owns a `Failure`, `ProjectProfile`, evidence, hypotheses, probes, ranked root causes, remediations, verification steps, and a `DiagnosticGraph`. All collections are immutable snapshots and every identifier is stable within a session.

`Evidence` records type, source, redacted summary, strength, sensitivity, collection time, and sanitized metadata. `EvidenceSource` records source kind and locator. `Hypothesis` records lifecycle status, categorical confidence, supporting and contradicting evidence IDs, and available/executed probe IDs. A `RootCauseCandidate` must reference a hypothesis and the evidence path supporting its rank.

## Hypothesis lifecycle

`UNTESTED -> TESTING -> SUPPORTED | WEAKLY_SUPPORTED | INCONCLUSIVE | CONTRADICTED | RULED_OUT | CONFIRMED`. Only an appropriate causal verification can produce `CONFIRMED`. Direct and independent observations can produce `VERY_HIGH` confidence without claiming causal confirmation.

## Confidence model

The engine scores explicit factors, not fabricated percentages: direct observation (+3), independent support (+2 each after the first), successful targeted probe (+3), causal verification (+5), specificity (+1), causal linkage (+2), contradicting evidence (-3 each), and unresolved prerequisite (-2). Mapping: <=0 VERY_LOW, 1–2 LOW, 3–4 MEDIUM, 5–7 HIGH, 8+ VERY_HIGH; causal verification plus no contradiction is CONFIRMED. Reports expose factors alongside the category.

## Graph

Nodes are typed (`FAILURE`, `EVIDENCE`, `HYPOTHESIS`, `PROBE`, `SOURCE`, `CONFIGURATION`, `DEPENDENCY`, `ENVIRONMENT`, `REMEDIATION`, `VERIFICATION`). Edges are typed (`SUPPORTS`, `CONTRADICTS`, `CAUSES`, `OBSERVED_AT`, `DERIVED_FROM`, `TESTED_BY`, `RULES_OUT`, `CORRELATES_WITH`, `INTRODUCED_BY`, `DEPENDS_ON`, `VERIFIED_BY`). Edges carry a redacted rationale. Referential integrity is validated when the graph is constructed.

## Provenance example

`ENV:FONU_API_KEY -> DERIVED_FROM -> E-002 -> SUPPORTS -> H-003 -> CAUSES -> FAILURE`. Querying `E-002` reveals its source, safe observation, collection time, and all consuming graph edges without exposing the value.
