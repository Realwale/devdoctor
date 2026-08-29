# Privacy

Default operation is local and offline-capable. DevDoctor sends no telemetry and no project data externally. Saved sessions contain only redacted evidence and are stored under the project `.devdoctor/` directory when persistence is requested. Users control deletion through normal filesystem tools.

Future external reasoning requires explicit consent and receives only a sanitized, size-limited context. Raw configuration values, environment values, credentials, source archives, and command output never enter that boundary.

Explicit HTTP reproduction keeps raw header values, request bodies, and query values out of diagnostic evidence. Evidence stores header names, a query-free URL, timing/status, and a bounded redacted response; response header values are not persisted. Inline `--header` and `--data` values can still appear in shell history or operating-system process arguments, so sensitive callers should prefer `--header-env` and `--data-file`.
