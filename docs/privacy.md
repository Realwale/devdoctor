# Privacy

Default operation is local and offline-capable. DevDoctor sends no telemetry and the bundled OpenTelemetry agent has trace, metric, and log exporters disabled. Saved sessions under `.devdoctor/` contain redacted, bounded evidence only.

Outcome correlation excludes request/response bodies, headers, cookies, and query strings by construction. Exception messages and stacks can still contain sensitive text; a bounded owner-only JFR file may hold those fields temporarily, then the engine redacts them before persistence and deletes its temporary recording. An imported JFR file is not modified or deleted and must be handled as sensitive by its owner.

A local installation cannot read a remote server without a separately authorized evidence path. Future external reasoning requires explicit consent and can receive only sanitized context.
