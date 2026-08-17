# Threat model

## Assets and trust boundaries

Source code, configuration, environment values, command output, logs, credentials, local paths, and diagnostic sessions are sensitive assets. Untrusted inputs include project files, logs containing terminal control codes, user commands, symlinks, crafted stack traces, high-entropy secrets, and future AI output. Collection is trusted only after normalization and redaction.

## Primary threats and controls

* Secret disclosure: key-name, structured-token, credential-URL, private-key, authorization, entropy, and multiline detection; values become characteristics such as presence/length/whitespace. Redaction precedes logs, JSON, persistence, reports, and AI.
* Command injection: only a user explicitly supplies `--command`; no AI-generated command executes. Process runs with bounded time/output, no interpolation by DevDoctor, and descendant cleanup on timeout.
* Resource exhaustion: byte/line/file-count limits, ignored build/VCS directories, bounded recursion, connect timeouts, and capped evidence.
* Path traversal/symlink escape: normalize paths against the project root; do not follow external symlinks for project collection.
* Terminal/log injection: strip control characters before rendering and use structured logging.
* Destructive probes: safety policy rejects `REQUIRES_PERMISSION` unless explicitly enabled and always rejects `PROHIBITED`; V1 ships no mutating probes.
* False diagnosis: competing hypotheses, negative evidence, specificity requirements, healthy fixtures, and explicit uncertainty.
* Supply chain: pinned dependency/plugin versions, Maven verification, minimal runtime dependencies, and documented release provenance.

## Residual risks

Novel secret formats and secrets embedded in arbitrary natural language may evade detectors; defense-in-depth therefore excludes raw values from evidence construction and tests whole output surfaces. User-supplied commands can themselves be destructive: CLI warnings make clear that DevDoctor executes exactly the explicit command, never a generated one.
