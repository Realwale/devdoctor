# Threat model

## Assets and boundaries

Source, configuration, environment values, command output, logs, exception metadata, local paths, JVM recordings, and diagnostic sessions are sensitive. Project files, logs, recordings, target JVMs, symlinks, and explicit commands are untrusted until normalized and redacted.

## Controls

* Secret disclosure: structured credential/token/private-key detection, characteristic-only environment evidence, redaction before persistence/reporting, and adversarial whole-output tests.
* Runtime privacy: no bodies, header values, cookies, or query values; no telemetry exporter; temporary JFR is owner-only, bounded to 64 MiB, and deleted after analysis.
* Runtime safety: generic JFR is used for an already-running uninstrumented JVM. Full outcome instrumentation is opt-in at application startup through `devdoctor run`, avoiding unsafe hot retransformation of a serving application.
* Command injection: only explicit `--command` or argument-list `devdoctor run -- ...` execution; no AI-generated command and no DevDoctor interpolation.
* Resource exhaustion: bounded duration/output/group counts/recording size, ignored build/VCS trees, connection timeouts, and capped evidence.
* Path/symlink escape: normalized project paths and no external symlink traversal during collection.
* False diagnosis: caught exceptions remain candidates; failed outcomes are separate very-high-strength evidence; healthy fixtures and scope-qualified uncertainty prevent overdiagnosis.
* Remote access: no implicit remote connector. A user must run DevDoctor on the host or supply an authorized recording/log.

## Residual risks

Novel secrets in arbitrary exception text may evade detection before the temporary JFR is deleted. Startup instrumentation adds application overhead and may be incompatible with unusual JVM/framework combinations; users can omit `devdoctor run` and use generic JFR/log evidence. Explicit user commands can themselves be destructive.
