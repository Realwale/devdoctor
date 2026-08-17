# Plugin development

Implement `DevDoctorPlugin` and return immutable lists of collectors, rules, and probes. `supports(ProjectProfile)` must be cheap and deterministic. Collectors must redact before creating evidence, rules generate competing hypotheses rather than final prose, and probes must be read-only, timeout-bound, safety classified, and deterministic where possible.

Plugins cannot depend on CLI classes or mutate a `DiagnosticSession`. The engine merges plugin contributions, validates unique IDs, applies policy, and builds a new immutable result. Compatibility follows semantic versioning of the plugin API.
